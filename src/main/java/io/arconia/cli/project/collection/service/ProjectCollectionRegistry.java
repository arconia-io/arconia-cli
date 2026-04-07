package io.arconia.cli.project.collection.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.json.JsonParser;
import io.arconia.cli.utils.SystemUtils;

/**
 * User-level registry of known project collections.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectCollectionRegistry(
    List<CollectionEntry> collections
) {

    public static final String CONFIG_SUBDIR = ".config/arconia/projects";

    public static final String FILENAME = "collections.json";

    public ProjectCollectionRegistry {
        Assert.notNull(collections, "collections cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CollectionEntry(
        String name,
        String ref,
        String digest,
        Instant lastFetchedAt
    ) {

        public CollectionEntry {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.hasText(ref, "ref cannot be null or empty");
            Assert.hasText(digest, "digest cannot be null or empty");
            Assert.notNull(lastFetchedAt, "lastFetchedAt cannot be null");
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private String name;
            private String ref;
            private String digest;
            private Instant lastFetchedAt;

            private Builder() {}

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder ref(String ref) {
                this.ref = ref;
                return this;
            }

            public Builder digest(String digest) {
                this.digest = digest;
                return this;
            }

            public Builder lastFetchedAt(Instant lastFetchedAt) {
                this.lastFetchedAt = lastFetchedAt;
                return this;
            }

            public CollectionEntry build() {
                return new CollectionEntry(name, ref, digest, lastFetchedAt);
            }

        }

    }

    public static void save(ProjectCollectionRegistry registry) throws IOException {
        Path path = registryPath();
        Files.createDirectories(path.getParent());
        String json = JsonParser.toJsonPrettyPrint(registry);
        Files.writeString(path, json + "\n");
    }

    public static ProjectCollectionRegistry load() throws IOException {
        Path path = registryPath();
        if (!Files.exists(path)) {
            return ProjectCollectionRegistry.builder().build();
        }

        String json = Files.readString(path);
        return JsonParser.fromJson(json, ProjectCollectionRegistry.class);
    }

    public static void addCollection(CollectionEntry entry) throws IOException {
        Assert.notNull(entry, "entry cannot be null");

        ProjectCollectionRegistry registry = load();
        List<CollectionEntry> updatedEntries = new ArrayList<>(registry.collections().stream()
                .filter(c -> !c.name().equals(entry.name()))
                .toList());
        updatedEntries.add(entry);

        save(ProjectCollectionRegistry.builder().collections(updatedEntries).build());
    }

    public static void removeCollection(String name) throws IOException {
        Assert.hasText(name, "name cannot be null or empty");

        ProjectCollectionRegistry registry = load();
        List<CollectionEntry> updatedEntries = new ArrayList<>(registry.collections().stream()
                .filter(c -> !c.name().equals(name))
                .toList());
        save(ProjectCollectionRegistry.builder().collections(updatedEntries).build());
    }


    @Nullable
    public static CollectionEntry findCollection(String name) throws IOException {
        Assert.hasText(name, "name cannot be null or empty");

        ProjectCollectionRegistry registry = load();
        return registry.collections().stream()
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static Path registryPath() {
        Path configDir = resolveConfigDirectory();
        return configDir.resolve(FILENAME);
    }

    private static Path resolveConfigDirectory() {
        // 1. arconia.config.home system property takes precedence on all platforms
        String arconiaConfigHome = System.getProperty("arconia.config.home");
        if (arconiaConfigHome != null && !arconiaConfigHome.isBlank()) {
            return Path.of(arconiaConfigHome);
        }

        // 2. XDG_CONFIG_HOME takes precedence on all platforms
        String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        if (xdgConfigHome != null && !xdgConfigHome.isBlank()) {
            return Path.of(xdgConfigHome, "arconia", "projects");
        }

        // 3. On Windows, use %APPDATA% (typically C:\Users\<user>\AppData\Roaming)
        if (SystemUtils.isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, "arconia", "projects");
            }
        }

        // 4. Default: ~/.config/arconia/projects (XDG standard for Linux/macOS)
        return Path.of(System.getProperty("user.home"), CONFIG_SUBDIR);
    }

    public static class Builder {

        private List<CollectionEntry> collections = new ArrayList<>();

        private Builder() {}

        public Builder collections(List<CollectionEntry> collections) {
            this.collections = collections;
            return this;
        }

        public ProjectCollectionRegistry build() {
            return new ProjectCollectionRegistry(collections);
        }

    }

}
