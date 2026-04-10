package io.arconia.cli.project.catalog.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.json.JsonParser;
import io.arconia.cli.utils.SystemUtils;

/**
 * User-level registry of known project catalogs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectCatalogRegistry(
    List<CatalogEntry> catalogs,
    @JsonSetter(nulls = Nulls.AS_EMPTY) boolean builtInCatalogDismissed
) {

    public static final String CONFIG_SUBDIR = ".config/arconia/projects";

    public static final String FILENAME = "catalogs.json";

    public ProjectCatalogRegistry {
        Assert.notNull(catalogs, "catalogs cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder mutate() {
        return new Builder()
                .catalogs(new ArrayList<>(catalogs))
                .builtInCatalogDismissed(builtInCatalogDismissed);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CatalogEntry(
        String name,
        String ref,
        String digest,
        Instant lastFetchedAt
    ) {

        public CatalogEntry {
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

            public CatalogEntry build() {
                return new CatalogEntry(name, ref, digest, lastFetchedAt);
            }

        }

    }

    public static void save(ProjectCatalogRegistry registry) throws IOException {
        Path path = registryPath();
        Files.createDirectories(path.getParent());
        String json = JsonParser.toJsonPrettyPrint(registry);
        Files.writeString(path, json + "\n");
    }

    public static ProjectCatalogRegistry load() throws IOException {
        Path path = registryPath();
        if (!Files.exists(path)) {
            return ProjectCatalogRegistry.builder().build();
        }

        String json = Files.readString(path);
        return JsonParser.fromJson(json, ProjectCatalogRegistry.class);
    }

    public static void addCatalog(CatalogEntry entry) throws IOException {
        Assert.notNull(entry, "entry cannot be null");

        ProjectCatalogRegistry registry = load();
        List<CatalogEntry> updatedEntries = new ArrayList<>(registry.catalogs().stream()
                .filter(c -> !c.name().equals(entry.name()))
                .toList());
        updatedEntries.add(entry);

        save(registry.mutate().catalogs(updatedEntries).build());
    }

    public static void removeCatalog(String name) throws IOException {
        Assert.hasText(name, "name cannot be null or empty");

        ProjectCatalogRegistry registry = load();
        List<CatalogEntry> updatedEntries = new ArrayList<>(registry.catalogs().stream()
                .filter(c -> !c.name().equals(name))
                .toList());
        save(registry.mutate().catalogs(updatedEntries).build());
    }


    @Nullable
    public static CatalogEntry findCatalog(String name) throws IOException {
        Assert.hasText(name, "name cannot be null or empty");

        ProjectCatalogRegistry registry = load();
        return registry.catalogs().stream()
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

        private List<CatalogEntry> catalogs = new ArrayList<>();
        private boolean builtInCatalogDismissed = false;

        private Builder() {}

        public Builder catalogs(List<CatalogEntry> catalogs) {
            this.catalogs = catalogs;
            return this;
        }

        public Builder builtInCatalogDismissed(boolean builtInCatalogDismissed) {
            this.builtInCatalogDismissed = builtInCatalogDismissed;
            return this;
        }

        public ProjectCatalogRegistry build() {
            return new ProjectCatalogRegistry(catalogs, builtInCatalogDismissed);
        }

    }

}
