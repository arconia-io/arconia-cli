package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.utils.JsonUtils;
import io.arconia.cli.utils.SystemUtils;

/**
 * Local cache of catalog contents, stored under the platform-appropriate cache directory.
 * <p>
 * Cache location:
 * <ul>
 *   <li>Linux/macOS: {@code ~/.cache/arconia/skills/<catalog-name>.json} (XDG standard)</li>
 *   <li>Windows: {@code %LOCALAPPDATA%\arconia\skills\<catalog-name>.json}</li>
 *   <li>Overridden by {@code XDG_CACHE_HOME} if set on any platform</li>
 * </ul>
 * <p>
 * The cache is disposable — it can be safely deleted at any time and will be
 * regenerated on the next fetch.
 */
public final class SkillCatalogCache {

    /**
     * Default cache TTL: 24 hours.
     */
    public static final Duration DEFAULT_TTL = Duration.ofHours(24);

    /**
     * The subdirectory under the user's cache home for Arconia skill caches.
     */
    public static final String CACHE_SUBDIR = ".cache/arconia/skills";

    private SkillCatalogCache() {}

    /**
     * A cached catalog entry containing the skills list and metadata.
     *
     * @param catalogName the catalog name/alias
     * @param catalogVersion the catalog version (may be {@code null})
     * @param description the catalog description (may be {@code null})
     * @param skills the list of cached skill entries
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CachedCatalog(
        String catalogName,
        @Nullable String catalogVersion,
        @Nullable String description,
        List<CachedSkill> skills
    ) {

        public CachedCatalog {
            Assert.hasText(catalogName, "catalogName cannot be null or be empty");
            Assert.notNull(skills, "skills cannot be null");
        }

    }

    /**
     * A cached skill entry within a catalog.
     *
     * @param name the skill name
     * @param version the skill version (may be {@code null})
     * @param description the skill description (may be {@code null})
     * @param ref the full OCI reference for the skill
     * @param digest the OCI digest of the skill manifest
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CachedSkill(
        String name,
        @Nullable String version,
        @Nullable String description,
        @Nullable String ref,
        @Nullable String digest
    ) {

        public CachedSkill {
            Assert.hasText(name, "name cannot be null or be empty");
        }

    }

    /**
     * Resolves the cache directory for Arconia skills.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>{@code XDG_CACHE_HOME/arconia/skills/} if the env var is set</li>
     *   <li>Windows: {@code %LOCALAPPDATA%\arconia\skills\}</li>
     *   <li>Fallback: {@code ~/.cache/arconia/skills/}</li>
     * </ol>
     *
     * @return the cache directory path
     */
    public static Path cacheDir() {
        // 1. XDG_CACHE_HOME takes precedence on all platforms
        String xdgCacheHome = System.getenv("XDG_CACHE_HOME");
        if (xdgCacheHome != null && !xdgCacheHome.isBlank()) {
            return Path.of(xdgCacheHome, "arconia", "skills");
        }

        // 2. On Windows, use %LOCALAPPDATA% (typically C:\Users\<user>\AppData\Local)
        if (SystemUtils.isWindows()) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                return Path.of(localAppData, "arconia", "skills");
            }
        }

        // 3. Default: ~/.cache/arconia/skills (XDG standard for Linux/macOS)
        return Path.of(System.getProperty("user.home"), CACHE_SUBDIR);
    }

    /**
     * Returns the path to the cache file for a given catalog name.
     *
     * @param catalogName the catalog name
     * @return the cache file path
     */
    public static Path cacheFile(String catalogName) {
        Assert.hasText(catalogName, "catalogName must not be empty");
        return cacheDir().resolve(catalogName + ".json");
    }

    /**
     * Loads a cached catalog by name.
     *
     * @param catalogName the catalog name
     * @return the cached catalog, or {@code null} if not cached
     * @throws IOException if the cache file cannot be read
     */
    @Nullable
    public static CachedCatalog load(String catalogName) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be empty");

        Path path = cacheFile(catalogName);
        if (!Files.exists(path)) {
            return null;
        }

        String json = Files.readString(path);
        return JsonUtils.getJsonMapper().readValue(json, CachedCatalog.class);
    }

    /**
     * Saves a catalog to the cache.
     *
     * @param catalogName the catalog name (used as the filename)
     * @param catalog the catalog data to cache
     * @throws IOException if the cache file cannot be written
     */
    public static void save(String catalogName, CachedCatalog catalog) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or be empty");
        Assert.notNull(catalog, "catalog cannot be null");

        Path path = cacheFile(catalogName);
        Files.createDirectories(path.getParent());
        String json = JsonUtils.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(catalog);
        Files.writeString(path, json + "\n");
    }

    /**
     * Deletes the cache file for a given catalog.
     *
     * @param catalogName the catalog name
     * @throws IOException if the file cannot be deleted
     */
    public static void delete(String catalogName) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be empty");
        Files.deleteIfExists(cacheFile(catalogName));
    }

    /**
     * Checks whether a catalog's cache is still fresh based on the
     * {@code lastFetchedAt} timestamp in the registry entry.
     *
     * @param lastFetchedAt the ISO 8601 timestamp from the registry entry, or {@code null}
     * @param ttl the maximum age before the cache is considered stale
     * @return {@code true} if the cache is fresh and can be used
     */
    public static boolean isFresh(@Nullable String lastFetchedAt, Duration ttl) {
        if (lastFetchedAt == null || lastFetchedAt.isBlank()) {
            return false;
        }
        try {
            Instant fetchedTime = Instant.parse(lastFetchedAt);
            return Instant.now().isBefore(fetchedTime.plus(ttl));
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts a {@link SkillCatalogReader.CatalogInfo} (fetched from registry)
     * into a {@link CachedCatalog} for local storage.
     *
     * @param info the catalog info from the registry
     * @return the cached catalog representation
     */
    public static CachedCatalog fromCatalogInfo(SkillCatalogReader.CatalogInfo info) {
        Assert.notNull(info, "info cannot be null");

        List<CachedSkill> skills = info.skills().stream()
            .map(s -> new CachedSkill(s.name(), s.version(), s.description(), s.ref(), s.digest()))
            .toList();

        return new CachedCatalog(
            info.catalogName() != null ? info.catalogName() : "unknown",
            info.catalogVersion(),
            info.description(),
            skills
        );
    }

    /**
     * Finds a skill by name across a cached catalog.
     *
     * @param catalog the cached catalog to search
     * @param skillName the skill name to find
     * @return the matching skill, or {@code null} if not found
     */
    @Nullable
    public static CachedSkill findSkill(CachedCatalog catalog, String skillName) {
        Assert.notNull(catalog, "catalog cannot be null");
        Assert.hasText(skillName, "skillName cannot be null or empty");

        return catalog.skills().stream()
            .filter(s -> s.name().equals(skillName))
            .findFirst()
            .orElse(null);
    }

}
