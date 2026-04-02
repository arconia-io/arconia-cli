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
 * Manages the lifecycle of registered skill collections: registration, fetching,
 * caching, update checking, removal, and skill resolution.
 * <p>
 * This class is the single facade for all collection operations. The CLI command
 * layer interacts with collections exclusively through this service, without
 * knowledge of how collection data is cached, stored, or fetched from the registry.
 */
public final class SkillCollectionService {

    private final Registry registry;

    /**
     * Creates a new collection service.
     *
     * @param registry the ORAS registry client
     */
    public SkillCollectionService(Registry registry) {
        Assert.notNull(registry, "registry cannot be null");
        this.registry = registry;
    }

    // ---- Collection view model ----

    /**
     * A unified view of a collection's contents, abstracting over whether
     * the data came from a live registry fetch or the local cache.
     *
     * @param collectionName the collection name or alias
     * @param collectionVersion the collection version (may be {@code null})
     * @param description the collection description (may be {@code null})
     * @param skills the list of skill summaries
     */
    public record CollectionView(
        String collectionName,
        @Nullable String collectionVersion,
        @Nullable String description,
        List<SkillSummary> skills
    ) {
        public CollectionView {
            Assert.hasText(collectionName, "collectionName cannot be null or empty");
            Assert.notNull(skills, "skills cannot be null");
        }
    }

    /**
     * A summary of a single skill within a collection, exposing only the
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

    // ---- Collection registration ----

    /**
     * A summary of a registered collection entry, exposing only what the
     * command layer needs for display.
     *
     * @param name the collection alias
     * @param ref the OCI reference
     */
    public record RegisteredCollection(
        String name,
        String ref
    ) {
        public RegisteredCollection {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.hasText(ref, "ref cannot be null or empty");
        }
    }

    /**
     * Registers a collection by fetching its contents from the OCI registry,
     * caching them locally, and saving the registry entry with full metadata.
     * <p>
     * This is an atomic operation: the collection is only registered if the fetch
     * succeeds. If the OCI reference cannot be resolved, no registry entry is
     * created and an exception is thrown.
     *
     * @param collectionName the alias for the collection
     * @param collectionRef the OCI reference for the collection
     * @return {@code true} if a collection with that name was already registered (update),
     *         {@code false} for a new registration
     * @throws IOException if the collection cannot be fetched, cached, or saved
     */
    public boolean addCollection(String collectionName, String collectionRef) throws IOException {
        Assert.hasText(collectionName, "collectionName cannot be null or empty");
        Assert.hasText(collectionRef, "collectionRef cannot be null or empty");

        SkillCollectionRegistry collectionRegistry = SkillCollectionRegistry.load();
        boolean existed = collectionRegistry.findCollection(collectionName) != null;

        // Fetch from registry first — fail fast if the ref is invalid
        SkillCollectionReader reader = new SkillCollectionReader(registry);
        SkillCollectionReader.CollectionInfo info = reader.read(collectionRef);

        // Cache the collection contents
        SkillCollectionCache.CachedCollection cached = SkillCollectionCache.fromCollectionInfo(info);
        SkillCollectionCache.save(collectionName, cached);

        // Extract digest from the already-fetched index
        String digest = info.index().getDescriptor().getDigest();
        String now = Instant.now().toString();

        // Register with full metadata (ref + digest + lastFetchedAt) in one write
        collectionRegistry = collectionRegistry.addCollection(
            new SkillCollectionRegistry.CollectionEntry(collectionName, collectionRef, digest, now));
        collectionRegistry.save();

        return existed;
    }

    /**
     * Removes a registered collection and cleans up its cache.
     *
     * @param collectionName the collection alias to remove
     * @return the removed collection's summary, or {@code null} if no collection with that name existed
     * @throws IOException if the removal cannot be saved
     */
    @Nullable
    public RegisteredCollection removeCollection(String collectionName) throws IOException {
        Assert.hasText(collectionName, "collectionName cannot be null or empty");

        SkillCollectionRegistry collectionRegistry = SkillCollectionRegistry.load();
        SkillCollectionRegistry.CollectionEntry existing = collectionRegistry.findCollection(collectionName);

        if (existing == null) {
            return null;
        }

        collectionRegistry = collectionRegistry.removeCollection(collectionName);
        collectionRegistry.save();

        SkillCollectionCache.delete(collectionName);

        return new RegisteredCollection(existing.name(), existing.ref());
    }

    /**
     * Returns all registered collections.
     *
     * @return the list of registered collection summaries
     * @throws IOException if the registry cannot be read
     */
    public List<RegisteredCollection> listRegisteredCollections() throws IOException {
        SkillCollectionRegistry collectionRegistry = SkillCollectionRegistry.load();
        return collectionRegistry.collections().stream()
            .map(e -> new RegisteredCollection(e.name(), e.ref()))
            .toList();
    }

    /**
     * Returns the file system paths for verbose diagnostics.
     *
     * @param collectionName the collection alias
     * @return a record containing the config and cache file paths
     */
    public CollectionPaths getCollectionPaths(String collectionName) {
        return new CollectionPaths(
            SkillCollectionRegistry.registryPath().toString(),
            SkillCollectionCache.cacheFile(collectionName).toString()
        );
    }

    /**
     * File system paths for collection configuration and cache files.
     *
     * @param configPath the path to the collection registry config file
     * @param cachePath the path to the cached collection file
     */
    public record CollectionPaths(String configPath, String cachePath) {}

    // ---- Fetching and caching ----

    /**
     * Fetches a collection from the OCI registry and caches it locally.
     * Also updates the {@code lastFetchedAt} timestamp and digest in the collection registry.
     *
     * @param collectionName the collection alias
     * @param collectionRef the OCI reference for the collection
     * @throws IOException if the collection cannot be fetched or cached
     */
    public void fetchAndCache(String collectionName, String collectionRef) throws IOException {
        Assert.hasText(collectionName, "collectionName cannot be null or empty");
        Assert.hasText(collectionRef, "collectionRef cannot be null or empty");

        SkillCollectionReader reader = new SkillCollectionReader(registry);
        SkillCollectionReader.CollectionInfo info = reader.read(collectionRef);

        // Cache the collection contents
        SkillCollectionCache.CachedCollection cached = SkillCollectionCache.fromCollectionInfo(info);
        SkillCollectionCache.save(collectionName, cached);

        // Extract the digest from the already-fetched index (avoids a duplicate registry call)
        String digest = info.index().getDescriptor().getDigest();

        // Update the digest and lastFetchedAt timestamp in the collection registry
        String now = Instant.now().toString();
        SkillCollectionRegistry collectionRegistry = SkillCollectionRegistry.load();
        SkillCollectionRegistry.CollectionEntry existing = collectionRegistry.findCollection(collectionName);
        if (existing != null) {
            SkillCollectionRegistry.CollectionEntry updated = existing.mutate()
                .digest(digest)
                .lastFetchedAt(now)
                .build();
            collectionRegistry = collectionRegistry.addCollection(updated);
            collectionRegistry.save();
        }
    }

    /**
     * Fetches a collection directly from the registry (no caching).
     * Used for ad-hoc {@code --ref} queries.
     *
     * @param collectionRef the OCI reference for the collection
     * @return the collection view
     */
    public CollectionView fetchCollectionFromRef(String collectionRef) {
        Assert.hasText(collectionRef, "collectionRef cannot be null or empty");

        SkillCollectionReader reader = new SkillCollectionReader(registry);
        SkillCollectionReader.CollectionInfo info = reader.read(collectionRef);

        return toCollectionView(
            info.collectionName() != null ? info.collectionName() : collectionRef,
            info.collectionVersion(),
            info.description(),
            info.skills().stream()
                .map(s -> new SkillSummary(s.name(), s.version(), s.description(), s.ref(), s.digest()))
                .toList()
        );
    }

    /**
     * The result of attempting to get a registered collection's contents.
     */
    public sealed interface CollectionFetchResult {

        /**
         * The collection was successfully loaded (from cache or fresh fetch).
         *
         * @param view the collection view
         * @param source describes where the data came from (e.g., "cache", "fresh fetch")
         */
        record Success(CollectionView view, String source) implements CollectionFetchResult {}

        /**
         * The collection could not be loaded at all.
         *
         * @param errorMessage the error description
         */
        record Failure(String errorMessage) implements CollectionFetchResult {}
    }

    /**
     * Gets the contents of a registered collection, using the cache with
     * staleness-aware refresh logic.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>If a fresh cache exists, return it immediately</li>
     *   <li>If the cache is stale, attempt a refresh; on failure, return stale data</li>
     *   <li>If no cache exists, fetch from the registry</li>
     * </ol>
     *
     * @param collectionName the collection alias
     * @return the fetch result
     * @throws IOException if the collection is not registered
     */
    public CollectionFetchResult getRegisteredCollection(String collectionName) throws IOException {
        Assert.hasText(collectionName, "collectionName cannot be null or empty");

        SkillCollectionRegistry collectionRegistry = SkillCollectionRegistry.load();
        SkillCollectionRegistry.CollectionEntry entry = collectionRegistry.findCollection(collectionName);

        if (entry == null) {
            throw new IllegalArgumentException(
                "No collection named '%s' is registered. Use 'collection add' to register one.".formatted(collectionName));
        }

        // Try the cache first
        SkillCollectionCache.CachedCollection cached = SkillCollectionCache.load(collectionName);

        if (cached != null) {
            // Check if the cache is still fresh
            if (SkillCollectionCache.isFresh(entry.lastFetchedAt(), SkillCollectionCache.DEFAULT_TTL)) {
                return new CollectionFetchResult.Success(
                    toCollectionViewFromCache(collectionName, cached),
                    "cache (fetched %s)".formatted(entry.lastFetchedAt()));
            }

            // Cache is stale — attempt a refresh, fall back to stale data on failure
            SkillCollectionCache.CachedCollection staleCache = cached;
            try {
                fetchAndCache(collectionName, entry.ref());
                cached = SkillCollectionCache.load(collectionName);
            }
            catch (Exception e) {
                // Refresh failed — return stale data
                return new CollectionFetchResult.Success(
                    toCollectionViewFromCache(collectionName, staleCache),
                    "stale cache (refresh failed: %s)".formatted(e.getMessage()));
            }

            // Return refreshed data, or fall back to stale data if reload failed
            if (cached != null) {
                return new CollectionFetchResult.Success(
                    toCollectionViewFromCache(collectionName, cached),
                    "refreshed");
            }
            return new CollectionFetchResult.Success(
                toCollectionViewFromCache(collectionName, staleCache),
                "stale cache (reload failed after refresh)");
        }

        // No cache yet — perform initial fetch
        try {
            fetchAndCache(collectionName, entry.ref());

            cached = SkillCollectionCache.load(collectionName);
            if (cached != null) {
                return new CollectionFetchResult.Success(
                    toCollectionViewFromCache(collectionName, cached),
                    "initial fetch");
            }
            return new CollectionFetchResult.Failure("Fetch succeeded but cache could not be loaded.");
        }
        catch (Exception e) {
            return new CollectionFetchResult.Failure(
                "Failed to fetch collection '%s' and no cached data available: %s".formatted(collectionName, e.getMessage()));
        }
    }

    // ---- Update checking ----

    /**
     * The result of checking a collection for updates.
     */
    public sealed interface CollectionUpdateResult {

        /**
         * The collection is already up to date.
         *
         * @param name the collection name
         * @param tag the current tag
         */
        record UpToDate(String name, String tag) implements CollectionUpdateResult {}

        /**
         * A newer semver version is available.
         *
         * @param name the collection name
         * @param currentTag the currently registered tag
         * @param newestTag the tag to update to
         * @param newestRef the full OCI reference for the update
         */
        record NewVersionAvailable(String name, String currentTag, String newestTag, String newestRef) implements CollectionUpdateResult {}

        /**
         * The digest has changed for the same tag (mutable tag refresh).
         *
         * @param name the collection name
         * @param tag the current tag
         * @param ref the OCI reference
         */
        record DigestChanged(String name, String tag, String ref) implements CollectionUpdateResult {}
    }

    /**
     * Checks whether an update is available for a registered collection.
     *
     * @param collectionName the collection alias
     * @return the result of the update check
     * @throws IOException if the registry cannot be read
     * @throws IllegalArgumentException if the collection is not registered
     */
    public CollectionUpdateResult checkForUpdate(String collectionName) throws IOException {
        Assert.hasText(collectionName, "collectionName cannot be null or empty");

        SkillCollectionRegistry collectionRegistry = SkillCollectionRegistry.load();
        SkillCollectionRegistry.CollectionEntry entry = collectionRegistry.findCollection(collectionName);

        if (entry == null) {
            throw new IllegalArgumentException(
                "No collection named '%s' is registered.".formatted(collectionName));
        }

        return checkForUpdate(entry);
    }

    /**
     * Applies a collection update: updates the registry ref if the tag changed, then re-fetches and caches.
     *
     * @param collectionName the collection alias
     * @param newestRef the new OCI reference (may be the same as current for digest-only changes)
     * @throws IOException if the update cannot be applied
     */
    public void applyUpdate(String collectionName, String newestRef) throws IOException {
        Assert.hasText(collectionName, "collectionName cannot be null or empty");
        Assert.hasText(newestRef, "newestRef cannot be null or empty");

        SkillCollectionRegistry collectionRegistry = SkillCollectionRegistry.load();
        SkillCollectionRegistry.CollectionEntry entry = collectionRegistry.findCollection(collectionName);

        if (entry != null && !newestRef.equals(entry.ref())) {
            collectionRegistry = collectionRegistry.addCollection(
                new SkillCollectionRegistry.CollectionEntry(collectionName, newestRef, null, null));
            collectionRegistry.save();
        }

        fetchAndCache(collectionName, newestRef);
    }

    // ---- Skill resolution from collections ----

    /**
     * A match of a skill name to a specific collection.
     *
     * @param collectionName the name of the collection where the skill was found
     * @param skill the resolved skill summary
     */
    public record CollectionSkillMatch(String collectionName, SkillSummary skill) {
        public CollectionSkillMatch {
            Assert.hasText(collectionName, "collectionName cannot be null or empty");
            Assert.notNull(skill, "skill cannot be null");
        }
    }

    /**
     * Resolves a skill name from registered collections.
     * <p>
     * Searches the specified collection (or all registered collections if none specified)
     * for a skill with the given name.
     *
     * @param skillName the skill name to look up
     * @param collectionAlias the specific collection to search, or {@code null} to search all
     * @return the resolved skill match
     * @throws IOException if collections cannot be loaded
     * @throws IllegalArgumentException if the skill is not found or found in multiple collections
     */
    public CollectionSkillMatch resolveSkillFromCollection(String skillName, String collectionAlias) throws IOException {
        Assert.hasText(skillName, "skillName cannot be null or empty");

        SkillCollectionRegistry collectionRegistry = SkillCollectionRegistry.load();

        // Determine which collections to search
        List<SkillCollectionRegistry.CollectionEntry> toSearch;
        if (collectionAlias != null && !collectionAlias.isBlank()) {
            SkillCollectionRegistry.CollectionEntry entry = collectionRegistry.findCollection(collectionAlias);
            if (entry == null) {
                throw new IllegalArgumentException(
                    "No collection named '%s' is registered. Use 'collection add' first.".formatted(collectionAlias));
            }
            toSearch = List.of(entry);
        }
        else {
            toSearch = collectionRegistry.collections();
            if (toSearch.isEmpty()) {
                throw new IllegalArgumentException(
                    "No collections registered. Use 'collection add' to register one, or use --ref for direct OCI references.");
            }
        }

        // Search for the skill in collection caches
        List<CollectionSkillMatch> matches = new ArrayList<>();
        for (SkillCollectionRegistry.CollectionEntry collectionEntry : toSearch) {
            SkillCollectionCache.CachedCollection cached = SkillCollectionCache.load(collectionEntry.name());
            if (cached == null) {
                continue;
            }
            SkillCollectionCache.CachedSkill skill = SkillCollectionCache.findSkill(cached, skillName);
            if (skill != null) {
                matches.add(new CollectionSkillMatch(collectionEntry.name(), toSkillSummary(skill)));
            }
        }

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                "Skill '%s' not found in any registered collection. Use 'collection list' to browse available skills.".formatted(skillName));
        }

        if (matches.size() > 1 && (collectionAlias == null || collectionAlias.isBlank())) {
            String collectionNames = matches.stream()
                .map(CollectionSkillMatch::collectionName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
            throw new IllegalArgumentException(
                "Skill '%s' found in multiple collections: %s. Use --collection <alias> to disambiguate.".formatted(
                    skillName, collectionNames));
        }

        return matches.getFirst();
    }

    // ---- Internal helpers ----

    /**
     * Checks whether an update is available for the given collection entry.
     */
    private CollectionUpdateResult checkForUpdate(SkillCollectionRegistry.CollectionEntry entry) {
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
            return new CollectionUpdateResult.NewVersionAvailable(entry.name(), currentTag, newestTag, newestRef);
        }

        // 3. No higher semver tag — check if the current tag's digest changed
        String remoteDigest = registry.getIndex(containerRef).getDescriptor().getDigest();

        if (entry.digest() != null && entry.digest().equals(remoteDigest)) {
            return new CollectionUpdateResult.UpToDate(entry.name(), currentTag);
        }

        return new CollectionUpdateResult.DigestChanged(entry.name(), currentTag, entry.ref());
    }

    /**
     * Converts a cached collection into a {@link CollectionView}.
     */
    private CollectionView toCollectionViewFromCache(String collectionName, SkillCollectionCache.CachedCollection cached) {
        List<SkillSummary> skills = cached.skills().stream()
            .map(this::toSkillSummary)
            .toList();
        return new CollectionView(collectionName, cached.collectionVersion(), cached.description(), skills);
    }

    /**
     * Creates a {@link CollectionView} from individual fields.
     */
    private CollectionView toCollectionView(String name, @Nullable String version,
                                       @Nullable String description, List<SkillSummary> skills) {
        return new CollectionView(name, version, description, skills);
    }

    /**
     * Converts a {@link SkillCollectionCache.CachedSkill} to a {@link SkillSummary}.
     */
    private SkillSummary toSkillSummary(SkillCollectionCache.CachedSkill cached) {
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
