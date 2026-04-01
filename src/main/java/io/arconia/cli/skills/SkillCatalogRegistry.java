package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.utils.JsonUtils;
import io.arconia.cli.utils.SystemUtils;

/**
 * User-level registry of known skill catalogs.
 * <p>
 * Stores catalog registrations in a platform-appropriate config directory:
 * <ul>
 *   <li>Linux/macOS: {@code ~/.config/arconia/skills/catalogs.json} (XDG standard)</li>
 *   <li>Windows: {@code %APPDATA%\arconia\skills\catalogs.json}</li>
 *   <li>Overridden by {@code XDG_CONFIG_HOME} if set on any platform</li>
 * </ul>
 * <p>
 * This is a CLI-level concern — not a project-level file — and is NOT intended
 * to be committed to version control. Catalog contents are always fetched live
 * from the OCI registry; this file only stores the registered aliases and refs.
 *
 * @param catalogs the list of registered catalog entries
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillCatalogRegistry(
    List<CatalogEntry> catalogs
) {

    /**
     * The filename for the catalog registry.
     */
    public static final String FILENAME = "catalogs.json";

    /**
     * The subdirectory under the user's config home where Arconia CLI stores
     * skill-related configuration files.
     */
    public static final String CONFIG_SUBDIR = ".config/arconia/skills";

    public SkillCatalogRegistry {
        Assert.notNull(catalogs, "catalogs cannot be null");
    }

    /**
     * Creates a new builder for {@link SkillCatalogRegistry}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder pre-populated with the values from this registry,
     * allowing selective modification.
     *
     * @return a pre-populated builder
     */
    public Builder mutate() {
        return new Builder()
            .catalogs(new ArrayList<>(catalogs));
    }

    /**
     * A registered catalog entry.
     *
     * @param name a short alias for the catalog (e.g., {@code "arconia"})
     * @param ref the OCI reference for the catalog (e.g., {@code "ghcr.io/arconia-io/skills-catalog:latest"})
     * @param digest the immutable manifest digest from the last fetch (e.g., {@code "sha256:abc..."}), or {@code null} if never fetched
     * @param lastFetchedAt ISO 8601 timestamp of the last successful fetch, or {@code null} if never fetched
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CatalogEntry(
        String name,
        String ref,
        @Nullable String digest,
        @Nullable String lastFetchedAt
    ) {

        public CatalogEntry {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.hasText(ref, "ref cannot be null or empty");
        }

        /**
         * Creates a new builder for {@link CatalogEntry}.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Returns a builder pre-populated with the values from this entry,
         * allowing selective modification.
         *
         * @return a pre-populated builder
         */
        public Builder mutate() {
            return new Builder()
                .name(name)
                .ref(ref)
                .digest(digest)
                .lastFetchedAt(lastFetchedAt);
        }

        /**
         * Builder for {@link CatalogEntry}.
         */
        public static class Builder {

            private String name;
            private String ref;
            private @Nullable String digest;
            private @Nullable String lastFetchedAt;

            private Builder() {}

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder ref(String ref) {
                this.ref = ref;
                return this;
            }

            public Builder digest(@Nullable String digest) {
                this.digest = digest;
                return this;
            }

            public Builder lastFetchedAt(@Nullable String lastFetchedAt) {
                this.lastFetchedAt = lastFetchedAt;
                return this;
            }

            public CatalogEntry build() {
                return new CatalogEntry(name, ref, digest, lastFetchedAt);
            }

        }

    }

    /**
     * Creates an empty registry.
     *
     * @return an empty catalog registry
     */
    public static SkillCatalogRegistry empty() {
        return new SkillCatalogRegistry(new ArrayList<>());
    }

    /**
     * Resolves the path to the catalog registry file.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>{@code XDG_CONFIG_HOME/arconia/skills/} if the env var is set</li>
     *   <li>Windows: {@code %APPDATA%\arconia\skills\} (via {@code APPDATA} env var)</li>
     *   <li>Fallback: {@code ~/.config/arconia/skills/}</li>
     * </ol>
     *
     * @return the path to the catalog registry file
     */
    public static Path registryPath() {
        Path configDir = resolveConfigDir();
        return configDir.resolve(FILENAME);
    }

    /**
     * Resolves the platform-appropriate config directory for Arconia skills.
     */
    private static Path resolveConfigDir() {
        // 1. XDG_CONFIG_HOME takes precedence on all platforms
        String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        if (xdgConfigHome != null && !xdgConfigHome.isBlank()) {
            return Path.of(xdgConfigHome, "arconia", "skills");
        }

        // 2. On Windows, use %APPDATA% (typically C:\Users\<user>\AppData\Roaming)
        if (SystemUtils.isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, "arconia", "skills");
            }
        }

        // 3. Default: ~/.config/arconia/skills (XDG standard for Linux/macOS)
        return Path.of(System.getProperty("user.home"), CONFIG_SUBDIR);
    }

    /**
     * Loads the catalog registry from the user's config directory.
     * Returns an empty registry if the file does not exist.
     *
     * @return the loaded registry
     * @throws IOException if the file cannot be read
     */
    public static SkillCatalogRegistry load() throws IOException {
        Path path = registryPath();
        if (!Files.exists(path)) {
            return empty();
        }

        String json = Files.readString(path);
        return JsonUtils.getJsonMapper().readValue(json, SkillCatalogRegistry.class);
    }

    /**
     * Saves this registry to the user's config directory.
     * Creates parent directories if they do not exist.
     *
     * @throws IOException if the file cannot be written
     */
    public void save() throws IOException {
        Path path = registryPath();
        Files.createDirectories(path.getParent());
        String json = JsonUtils.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        Files.writeString(path, json + "\n");
    }

    /**
     * Returns a new registry with the given catalog added or updated.
     * If a catalog with the same name already exists, it is replaced.
     *
     * @param entry the catalog entry to add
     * @return a new registry with the catalog added
     */
    public SkillCatalogRegistry addCatalog(CatalogEntry entry) {
        Assert.notNull(entry, "entry cannot be null");

        List<CatalogEntry> updated = new ArrayList<>(catalogs.stream()
            .filter(c -> !c.name().equals(entry.name()))
            .toList());
        updated.add(entry);
        return new SkillCatalogRegistry(updated);
    }

    /**
     * Returns a new registry with the named catalog removed.
     *
     * @param name the catalog name to remove
     * @return a new registry without the named catalog
     */
    public SkillCatalogRegistry removeCatalog(String name) {
        Assert.hasText(name, "name cannot be null or empty");

        List<CatalogEntry> updated = new ArrayList<>(catalogs.stream()
            .filter(c -> !c.name().equals(name))
            .toList());
        return new SkillCatalogRegistry(updated);
    }

    /**
     * Finds a catalog entry by name.
     *
     * @param name the catalog name
     * @return the matching entry, or {@code null} if not found
     */
    @Nullable
    public CatalogEntry findCatalog(String name) {
        Assert.hasText(name, "name cannot be null or empty");

        return catalogs.stream()
            .filter(c -> c.name().equals(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * Builder for {@link SkillCatalogRegistry}.
     */
    public static class Builder {

        private List<CatalogEntry> catalogs = new ArrayList<>();

        private Builder() {}

        public Builder catalogs(List<CatalogEntry> catalogs) {
            this.catalogs = catalogs;
            return this;
        }

        public SkillCatalogRegistry build() {
            return new SkillCatalogRegistry(catalogs);
        }

    }

}
