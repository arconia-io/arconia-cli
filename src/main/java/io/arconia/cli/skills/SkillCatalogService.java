package io.arconia.cli.skills;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import land.oras.ContainerRef;
import land.oras.Registry;

import io.arconia.cli.utils.OciUtils;
import io.arconia.cli.utils.SemverUtils;

/**
 * Manages the lifecycle of registered skill catalogs: registration, fetching,
 * caching, update checking, removal, and skill resolution.
 * <p>
 * This class is the single facade for all catalog operations. The CLI command
 * layer interacts with catalogs exclusively through this service, without
 * knowledge of how catalog data is cached, stored, or fetched from the registry.
 */
public final class SkillCatalogService {

    private final Registry registry;

    /**
     * Creates a new catalog service.
     *
     * @param registry the ORAS registry client
     */
    public SkillCatalogService(Registry registry) {
        Assert.notNull(registry, "registry cannot be null");
        this.registry = registry;
    }

    // ---- Catalog view model ----

    /**
     * A unified view of a catalog's contents, abstracting over whether
     * the data came from a live registry fetch or the local cache.
     *
     * @param catalogName the catalog name or alias
     * @param catalogVersion the catalog version (may be {@code null})
     * @param description the catalog description (may be {@code null})
     * @param skills the list of skill summaries
     */
    public record CatalogView(
        String catalogName,
        @Nullable String catalogVersion,
        @Nullable String description,
        List<SkillSummary> skills
    ) {
        public CatalogView {
            Assert.hasText(catalogName, "catalogName cannot be null or empty");
            Assert.notNull(skills, "skills cannot be null");
        }
    }

    /**
     * A summary of a single skill within a catalog, exposing only the
     * information needed by the command layer.
     *
     * @param name the skill name
     * @param version the skill version (may be {@code null})
     * @param description the skill description (may be {@code null})
     * @param ref the canonical OCI reference (may be {@code null})
     * @param digest the manifest digest (may be {@code null})
     */
    public record SkillSummary(
        String name,
        @Nullable String version,
        @Nullable String description,
        @Nullable String ref,
        @Nullable String digest
    ) {
        public SkillSummary {
            Assert.hasText(name, "name cannot be null or empty");
        }
    }

    // ---- Catalog registration ----

    /**
     * A summary of a registered catalog entry, exposing only what the
     * command layer needs for display.
     *
     * @param name the catalog alias
     * @param ref the OCI reference
     */
    public record RegisteredCatalog(
        String name,
        String ref
    ) {
        public RegisteredCatalog {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.hasText(ref, "ref cannot be null or empty");
        }
    }

    /**
     * Registers a catalog by fetching its contents from the OCI registry,
     * caching them locally, and saving the registry entry with full metadata.
     * <p>
     * This is an atomic operation: the catalog is only registered if the fetch
     * succeeds. If the OCI reference cannot be resolved, no registry entry is
     * created and an exception is thrown.
     *
     * @param catalogName the alias for the catalog
     * @param catalogRef the OCI reference for the catalog
     * @return {@code true} if a catalog with that name was already registered (update),
     *         {@code false} for a new registration
     * @throws IOException if the catalog cannot be fetched, cached, or saved
     */
    public boolean addCatalog(String catalogName, String catalogRef) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");
        Assert.hasText(catalogRef, "catalogRef cannot be null or empty");

        SkillCatalogRegistry catalogRegistry = SkillCatalogRegistry.load();
        boolean existed = catalogRegistry.findCatalog(catalogName) != null;

        // Fetch from registry first — fail fast if the ref is invalid
        SkillCatalogReader reader = new SkillCatalogReader(registry);
        SkillCatalogReader.CatalogInfo info = reader.read(catalogRef);

        // Cache the catalog contents
        SkillCatalogCache.CachedCatalog cached = SkillCatalogCache.fromCatalogInfo(info);
        SkillCatalogCache.save(catalogName, cached);

        // Extract digest from the already-fetched index
        String digest = info.index().getDescriptor().getDigest();
        String now = Instant.now().toString();

        // Register with full metadata (ref + digest + lastFetchedAt) in one write
        catalogRegistry = catalogRegistry.addCatalog(
            new SkillCatalogRegistry.CatalogEntry(catalogName, catalogRef, digest, now));
        catalogRegistry.save();

        return existed;
    }

    /**
     * Removes a registered catalog and cleans up its cache.
     *
     * @param catalogName the catalog alias to remove
     * @return the removed catalog's summary, or {@code null} if no catalog with that name existed
     * @throws IOException if the removal cannot be saved
     */
    @Nullable
    public RegisteredCatalog removeCatalog(String catalogName) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");

        SkillCatalogRegistry catalogRegistry = SkillCatalogRegistry.load();
        SkillCatalogRegistry.CatalogEntry existing = catalogRegistry.findCatalog(catalogName);

        if (existing == null) {
            return null;
        }

        catalogRegistry = catalogRegistry.removeCatalog(catalogName);
        catalogRegistry.save();

        SkillCatalogCache.delete(catalogName);

        return new RegisteredCatalog(existing.name(), existing.ref());
    }

    /**
     * Returns all registered catalogs.
     *
     * @return the list of registered catalog summaries
     * @throws IOException if the registry cannot be read
     */
    public List<RegisteredCatalog> listRegisteredCatalogs() throws IOException {
        SkillCatalogRegistry catalogRegistry = SkillCatalogRegistry.load();
        return catalogRegistry.catalogs().stream()
            .map(e -> new RegisteredCatalog(e.name(), e.ref()))
            .toList();
    }

    /**
     * Returns the file system paths for verbose diagnostics.
     *
     * @param catalogName the catalog alias
     * @return a record containing the config and cache file paths
     */
    public CatalogPaths getCatalogPaths(String catalogName) {
        return new CatalogPaths(
            SkillCatalogRegistry.registryPath().toString(),
            SkillCatalogCache.cacheFile(catalogName).toString()
        );
    }

    /**
     * File system paths for catalog configuration and cache files.
     *
     * @param configPath the path to the catalog registry config file
     * @param cachePath the path to the cached catalog file
     */
    public record CatalogPaths(String configPath, String cachePath) {}

    // ---- Fetching and caching ----

    /**
     * Fetches a catalog from the OCI registry and caches it locally.
     * Also updates the {@code lastFetchedAt} timestamp and digest in the catalog registry.
     *
     * @param catalogName the catalog alias
     * @param catalogRef the OCI reference for the catalog
     * @throws IOException if the catalog cannot be fetched or cached
     */
    public void fetchAndCache(String catalogName, String catalogRef) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");
        Assert.hasText(catalogRef, "catalogRef cannot be null or empty");

        SkillCatalogReader reader = new SkillCatalogReader(registry);
        SkillCatalogReader.CatalogInfo info = reader.read(catalogRef);

        // Cache the catalog contents
        SkillCatalogCache.CachedCatalog cached = SkillCatalogCache.fromCatalogInfo(info);
        SkillCatalogCache.save(catalogName, cached);

        // Extract the digest from the already-fetched index (avoids a duplicate registry call)
        String digest = info.index().getDescriptor().getDigest();

        // Update the digest and lastFetchedAt timestamp in the catalog registry
        String now = Instant.now().toString();
        SkillCatalogRegistry catalogRegistry = SkillCatalogRegistry.load();
        SkillCatalogRegistry.CatalogEntry existing = catalogRegistry.findCatalog(catalogName);
        if (existing != null) {
            SkillCatalogRegistry.CatalogEntry updated = existing.mutate()
                .digest(digest)
                .lastFetchedAt(now)
                .build();
            catalogRegistry = catalogRegistry.addCatalog(updated);
            catalogRegistry.save();
        }
    }

    /**
     * Fetches a catalog directly from the registry (no caching).
     * Used for ad-hoc {@code --ref} queries.
     *
     * @param catalogRef the OCI reference for the catalog
     * @return the catalog view
     */
    public CatalogView fetchCatalogFromRef(String catalogRef) {
        Assert.hasText(catalogRef, "catalogRef cannot be null or empty");

        SkillCatalogReader reader = new SkillCatalogReader(registry);
        SkillCatalogReader.CatalogInfo info = reader.read(catalogRef);

        return toCatalogView(
            info.catalogName() != null ? info.catalogName() : catalogRef,
            info.catalogVersion(),
            info.description(),
            info.skills().stream()
                .map(s -> new SkillSummary(s.name(), s.version(), s.description(), s.ref(), s.digest()))
                .toList()
        );
    }

    /**
     * The result of attempting to get a registered catalog's contents.
     */
    public sealed interface CatalogFetchResult {

        /**
         * The catalog was successfully loaded (from cache or fresh fetch).
         *
         * @param view the catalog view
         * @param source describes where the data came from (e.g., "cache", "fresh fetch")
         */
        record Success(CatalogView view, String source) implements CatalogFetchResult {}

        /**
         * The catalog could not be loaded at all.
         *
         * @param errorMessage the error description
         */
        record Failure(String errorMessage) implements CatalogFetchResult {}
    }

    /**
     * Gets the contents of a registered catalog, using the cache with
     * staleness-aware refresh logic.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>If a fresh cache exists, return it immediately</li>
     *   <li>If the cache is stale, attempt a refresh; on failure, return stale data</li>
     *   <li>If no cache exists, fetch from the registry</li>
     * </ol>
     *
     * @param catalogName the catalog alias
     * @return the fetch result
     * @throws IOException if the catalog is not registered
     */
    public CatalogFetchResult getRegisteredCatalog(String catalogName) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");

        SkillCatalogRegistry catalogRegistry = SkillCatalogRegistry.load();
        SkillCatalogRegistry.CatalogEntry entry = catalogRegistry.findCatalog(catalogName);

        if (entry == null) {
            throw new IllegalArgumentException(
                "No catalog named '%s' is registered. Use 'catalog add' to register one.".formatted(catalogName));
        }

        // Try the cache first
        SkillCatalogCache.CachedCatalog cached = SkillCatalogCache.load(catalogName);

        if (cached != null) {
            // Check if the cache is still fresh
            if (SkillCatalogCache.isFresh(entry.lastFetchedAt(), SkillCatalogCache.DEFAULT_TTL)) {
                return new CatalogFetchResult.Success(
                    toCatalogViewFromCache(catalogName, cached),
                    "cache (fetched %s)".formatted(entry.lastFetchedAt()));
            }

            // Cache is stale — attempt a refresh, fall back to stale data on failure
            SkillCatalogCache.CachedCatalog staleCache = cached;
            try {
                fetchAndCache(catalogName, entry.ref());
                cached = SkillCatalogCache.load(catalogName);
            }
            catch (Exception e) {
                // Refresh failed — return stale data
                return new CatalogFetchResult.Success(
                    toCatalogViewFromCache(catalogName, staleCache),
                    "stale cache (refresh failed: %s)".formatted(e.getMessage()));
            }

            // Return refreshed data, or fall back to stale data if reload failed
            if (cached != null) {
                return new CatalogFetchResult.Success(
                    toCatalogViewFromCache(catalogName, cached),
                    "refreshed");
            }
            return new CatalogFetchResult.Success(
                toCatalogViewFromCache(catalogName, staleCache),
                "stale cache (reload failed after refresh)");
        }

        // No cache yet — perform initial fetch
        try {
            fetchAndCache(catalogName, entry.ref());

            cached = SkillCatalogCache.load(catalogName);
            if (cached != null) {
                return new CatalogFetchResult.Success(
                    toCatalogViewFromCache(catalogName, cached),
                    "initial fetch");
            }
            return new CatalogFetchResult.Failure("Fetch succeeded but cache could not be loaded.");
        }
        catch (Exception e) {
            return new CatalogFetchResult.Failure(
                "Failed to fetch catalog '%s' and no cached data available: %s".formatted(catalogName, e.getMessage()));
        }
    }

    // ---- Update checking ----

    /**
     * The result of checking a catalog for updates.
     */
    public sealed interface CatalogUpdateResult {

        /**
         * The catalog is already up to date.
         *
         * @param name the catalog name
         * @param tag the current tag
         */
        record UpToDate(String name, String tag) implements CatalogUpdateResult {}

        /**
         * A newer semver version is available.
         *
         * @param name the catalog name
         * @param currentTag the currently registered tag
         * @param newestTag the tag to update to
         * @param newestRef the full OCI reference for the update
         */
        record NewVersionAvailable(String name, String currentTag, String newestTag, String newestRef) implements CatalogUpdateResult {}

        /**
         * The digest has changed for the same tag (mutable tag refresh).
         *
         * @param name the catalog name
         * @param tag the current tag
         * @param ref the OCI reference
         */
        record DigestChanged(String name, String tag, String ref) implements CatalogUpdateResult {}
    }

    /**
     * Checks whether an update is available for a registered catalog.
     *
     * @param catalogName the catalog alias
     * @return the result of the update check
     * @throws IOException if the registry cannot be read
     * @throws IllegalArgumentException if the catalog is not registered
     */
    public CatalogUpdateResult checkForUpdate(String catalogName) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");

        SkillCatalogRegistry catalogRegistry = SkillCatalogRegistry.load();
        SkillCatalogRegistry.CatalogEntry entry = catalogRegistry.findCatalog(catalogName);

        if (entry == null) {
            throw new IllegalArgumentException(
                "No catalog named '%s' is registered.".formatted(catalogName));
        }

        return checkForUpdate(entry);
    }

    /**
     * Applies a catalog update: updates the registry ref if the tag changed, then re-fetches and caches.
     *
     * @param catalogName the catalog alias
     * @param newestRef the new OCI reference (may be the same as current for digest-only changes)
     * @throws IOException if the update cannot be applied
     */
    public void applyUpdate(String catalogName, String newestRef) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");
        Assert.hasText(newestRef, "newestRef cannot be null or empty");

        SkillCatalogRegistry catalogRegistry = SkillCatalogRegistry.load();
        SkillCatalogRegistry.CatalogEntry entry = catalogRegistry.findCatalog(catalogName);

        if (entry != null && !newestRef.equals(entry.ref())) {
            catalogRegistry = catalogRegistry.addCatalog(
                new SkillCatalogRegistry.CatalogEntry(catalogName, newestRef, null, null));
            catalogRegistry.save();
        }

        fetchAndCache(catalogName, newestRef);
    }

    // ---- Skill resolution from catalogs ----

    /**
     * A match of a skill name to a specific catalog.
     *
     * @param catalogName the name of the catalog where the skill was found
     * @param skill the resolved skill summary
     */
    public record CatalogSkillMatch(String catalogName, SkillSummary skill) {
        public CatalogSkillMatch {
            Assert.hasText(catalogName, "catalogName cannot be null or empty");
            Assert.notNull(skill, "skill cannot be null");
        }
    }

    /**
     * Resolves a skill name from registered catalogs.
     * <p>
     * Searches the specified catalog (or all registered catalogs if none specified)
     * for a skill with the given name.
     *
     * @param skillName the skill name to look up
     * @param catalogAlias the specific catalog to search, or {@code null} to search all
     * @return the resolved skill match
     * @throws IOException if catalogs cannot be loaded
     * @throws IllegalArgumentException if the skill is not found or found in multiple catalogs
     */
    public CatalogSkillMatch resolveSkillFromCatalog(String skillName, String catalogAlias) throws IOException {
        Assert.hasText(skillName, "skillName cannot be null or empty");

        SkillCatalogRegistry catalogRegistry = SkillCatalogRegistry.load();

        // Determine which catalogs to search
        List<SkillCatalogRegistry.CatalogEntry> toSearch;
        if (catalogAlias != null && !catalogAlias.isBlank()) {
            SkillCatalogRegistry.CatalogEntry entry = catalogRegistry.findCatalog(catalogAlias);
            if (entry == null) {
                throw new IllegalArgumentException(
                    "No catalog named '%s' is registered. Use 'catalog add' first.".formatted(catalogAlias));
            }
            toSearch = List.of(entry);
        }
        else {
            toSearch = catalogRegistry.catalogs();
            if (toSearch.isEmpty()) {
                throw new IllegalArgumentException(
                    "No catalogs registered. Use 'catalog add' to register one, or use --ref for direct OCI references.");
            }
        }

        // Search for the skill in catalog caches
        List<CatalogSkillMatch> matches = new ArrayList<>();
        for (SkillCatalogRegistry.CatalogEntry catalogEntry : toSearch) {
            SkillCatalogCache.CachedCatalog cached = SkillCatalogCache.load(catalogEntry.name());
            if (cached == null) {
                continue;
            }
            SkillCatalogCache.CachedSkill skill = SkillCatalogCache.findSkill(cached, skillName);
            if (skill != null) {
                matches.add(new CatalogSkillMatch(catalogEntry.name(), toSkillSummary(skill)));
            }
        }

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                "Skill '%s' not found in any registered catalog. Use 'catalog list' to browse available skills.".formatted(skillName));
        }

        if (matches.size() > 1 && (catalogAlias == null || catalogAlias.isBlank())) {
            String catalogNames = matches.stream()
                .map(CatalogSkillMatch::catalogName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
            throw new IllegalArgumentException(
                "Skill '%s' found in multiple catalogs: %s. Use --catalog <alias> to disambiguate.".formatted(
                    skillName, catalogNames));
        }

        return matches.getFirst();
    }

    // ---- Internal helpers ----

    /**
     * Checks whether an update is available for the given catalog entry.
     */
    private CatalogUpdateResult checkForUpdate(SkillCatalogRegistry.CatalogEntry entry) {
        ContainerRef containerRef = ContainerRef.parse(entry.ref());
        String currentTag = containerRef.getTag() != null ? containerRef.getTag() : "latest";

        String repoBase = extractRepoBase(entry.ref());

        // 1. List all tags
        List<String> allTags = registry.getTags(containerRef).tags();

        // 2. Find the highest semver tag
        Optional<String> highestTag = SemverUtils.findHighestTag(allTags);

        if (highestTag.isPresent() && SemverUtils.isSemver(currentTag)
                && SemverUtils.compare(highestTag.get(), currentTag) > 0) {
            String newestTag = highestTag.get();
            String newestRef = "%s:%s".formatted(repoBase, newestTag);
            return new CatalogUpdateResult.NewVersionAvailable(entry.name(), currentTag, newestTag, newestRef);
        }

        // 3. No higher semver tag — check if the current tag's digest changed
        String remoteDigest = registry.getIndex(containerRef).getDescriptor().getDigest();

        if (entry.digest() != null && entry.digest().equals(remoteDigest)) {
            return new CatalogUpdateResult.UpToDate(entry.name(), currentTag);
        }

        return new CatalogUpdateResult.DigestChanged(entry.name(), currentTag, entry.ref());
    }

    /**
     * Converts a cached catalog into a {@link CatalogView}.
     */
    private CatalogView toCatalogViewFromCache(String catalogName, SkillCatalogCache.CachedCatalog cached) {
        List<SkillSummary> skills = cached.skills().stream()
            .map(this::toSkillSummary)
            .toList();
        return new CatalogView(catalogName, cached.catalogVersion(), cached.description(), skills);
    }

    /**
     * Creates a {@link CatalogView} from individual fields.
     */
    private CatalogView toCatalogView(String name, @Nullable String version,
                                       @Nullable String description, List<SkillSummary> skills) {
        return new CatalogView(name, version, description, skills);
    }

    /**
     * Converts a {@link SkillCatalogCache.CachedSkill} to a {@link SkillSummary}.
     */
    private SkillSummary toSkillSummary(SkillCatalogCache.CachedSkill cached) {
        return new SkillSummary(cached.name(), cached.version(), cached.description(), cached.ref(), cached.digest());
    }

    /**
     * Extracts the base repository reference (everything before the tag) from an OCI reference string.
     * Delegates to {@link OciUtils#extractRepoBase(String)}.
     */
    static String extractRepoBase(String ref) {
        return OciUtils.extractRepoBase(ref);
    }

}
