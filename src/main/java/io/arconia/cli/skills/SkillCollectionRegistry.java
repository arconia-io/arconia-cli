package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.json.JsonParser;
import io.arconia.cli.utils.SystemUtils;

/**
 * User-level registry of known skill collections.
 * <p>
 * Stores collection registrations in a platform-appropriate config directory:
 * <ul>
 *   <li>Linux/macOS: {@code ~/.config/arconia/skills/collections.json} (XDG standard)</li>
 *   <li>Windows: {@code %APPDATA%\arconia\skills\collections.json}</li>
 *   <li>Overridden by {@code XDG_CONFIG_HOME} if set on any platform</li>
 * </ul>
 * <p>
 * This is a CLI-level concern — not a project-level file — and is NOT intended
 * to be committed to version control. Collection contents are always fetched live
 * from the OCI registry; this file only stores the registered aliases and refs.
 *
 * @param collections the list of registered collection entries
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillCollectionRegistry(
    List<CollectionEntry> collections
) {

    /**
     * The filename for the collection registry.
     */
    public static final String FILENAME = "collections.json";

    /**
     * The subdirectory under the user's config home where Arconia CLI stores
     * skill-related configuration files.
     */
    public static final String CONFIG_SUBDIR = ".config/arconia/skills";

    public SkillCollectionRegistry {
        Assert.notNull(collections, "collections cannot be null");
    }

    /**
     * Creates a new builder for {@link SkillCollectionRegistry}.
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
            .collections(new ArrayList<>(collections));
    }

    /**
     * A registered collection entry.
     *
     * @param name a short alias for the collection (e.g., {@code "arconia"})
     * @param ref the OCI reference for the collection (e.g., {@code "ghcr.io/arconia-io/skills-collection:latest"})
     * @param digest the immutable manifest digest from the last fetch (e.g., {@code "sha256:abc..."}), or {@code null} if never fetched
     * @param lastFetchedAt ISO 8601 timestamp of the last successful fetch, or {@code null} if never fetched
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CollectionEntry(
        String name,
        String ref,
        @Nullable String digest,
        @Nullable String lastFetchedAt
    ) {

        public CollectionEntry {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.hasText(ref, "ref cannot be null or empty");
        }

        /**
         * Creates a new builder for {@link CollectionEntry}.
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
         * Builder for {@link CollectionEntry}.
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

            public CollectionEntry build() {
                return new CollectionEntry(name, ref, digest, lastFetchedAt);
            }

        }

    }

    /**
     * Creates an empty registry.
     *
     * @return an empty collection registry
     */
    public static SkillCollectionRegistry empty() {
        return new SkillCollectionRegistry(new ArrayList<>());
    }

    /**
     * Resolves the path to the collection registry file.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>{@code XDG_CONFIG_HOME/arconia/skills/} if the env var is set</li>
     *   <li>Windows: {@code %APPDATA%\arconia\skills\} (via {@code APPDATA} env var)</li>
     *   <li>Fallback: {@code ~/.config/arconia/skills/}</li>
     * </ol>
     *
     * @return the path to the collection registry file
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
     * Loads the collection registry from the user's config directory.
     * Returns an empty registry if the file does not exist.
     *
     * @return the loaded registry
     * @throws IOException if the file cannot be read
     */
    public static SkillCollectionRegistry load() throws IOException {
        Path path = registryPath();
        if (!Files.exists(path)) {
            return empty();
        }

        String json = Files.readString(path);
        return JsonParser.fromJson(json, SkillCollectionRegistry.class);
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
        String json = JsonParser.toJsonPrettyPrint(this);
        Files.writeString(path, json + "\n");
    }

    /**
     * Returns a new registry with the given collection added or updated.
     * If a collection with the same name already exists, it is replaced.
     *
     * @param entry the collection entry to add
     * @return a new registry with the collection added
     */
    public SkillCollectionRegistry addCollection(CollectionEntry entry) {
        Assert.notNull(entry, "entry cannot be null");

        List<CollectionEntry> updated = new ArrayList<>(collections.stream()
            .filter(c -> !c.name().equals(entry.name()))
            .toList());
        updated.add(entry);
        return new SkillCollectionRegistry(updated);
    }

    /**
     * Returns a new registry with the named collection removed.
     *
     * @param name the collection name to remove
     * @return a new registry without the named collection
     */
    public SkillCollectionRegistry removeCollection(String name) {
        Assert.hasText(name, "name cannot be null or empty");

        List<CollectionEntry> updated = new ArrayList<>(collections.stream()
            .filter(c -> !c.name().equals(name))
            .toList());
        return new SkillCollectionRegistry(updated);
    }

    /**
     * Finds a collection entry by name.
     *
     * @param name the collection name
     * @return the matching entry, or {@code null} if not found
     */
    @Nullable
    public CollectionEntry findCollection(String name) {
        Assert.hasText(name, "name cannot be null or empty");

        return collections.stream()
            .filter(c -> c.name().equals(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * Builder for {@link SkillCollectionRegistry}.
     */
    public static class Builder {

        private List<CollectionEntry> collections = new ArrayList<>();

        private Builder() {}

        public Builder collections(List<CollectionEntry> collections) {
            this.collections = collections;
            return this;
        }

        public SkillCollectionRegistry build() {
            return new SkillCollectionRegistry(collections);
        }

    }

}
