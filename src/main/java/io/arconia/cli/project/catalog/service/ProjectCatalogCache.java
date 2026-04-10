package io.arconia.cli.project.catalog.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import land.oras.Index;

import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.json.JsonParser;
import io.arconia.cli.project.oci.ProjectAnnotations;
import io.arconia.cli.utils.SystemUtils;

/**
 * Service for caching project catalogs on disk.
 */
public final class ProjectCatalogCache {

    public static final String CACHE_SUBDIR = ".cache/arconia/projects";

    private ProjectCatalogCache() {}

    public static void save(String catalogName, Index index) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");
        Assert.notNull(index, "index cannot be null");

        Path path = resolveCacheFileForCatalog(catalogName);
        Files.createDirectories(path.getParent());
        String json = JsonParser.toJsonPrettyPrint(index);
        Files.writeString(path, json + "\n");
    }

    @Nullable
    public static Index load(String catalogName) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");

        Path path = resolveCacheFileForCatalog(catalogName);
        if (!Files.exists(path)) {
            return null;
        }

        String json = Files.readString(path);
        return JsonParser.fromJson(json, Index.class);
    }

    public static void delete(String catalogName) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");
        Files.deleteIfExists(resolveCacheFileForCatalog(catalogName));
    }

    public static List<ProjectCatalogSummary> toProjectSummaries(Index index) {
        return index.getManifests().stream()
                .map(manifest -> ProjectCatalogSummary.builder()
                        .name(manifest.getAnnotations().get(ArtifactAnnotations.OCI_TITLE))
                        .description(manifest.getAnnotations().get(ArtifactAnnotations.OCI_DESCRIPTION))
                        .type(manifest.getAnnotations().get(ProjectAnnotations.PROJECT_TYPE))
                        .labels(List.of(manifest.getAnnotations().get(ProjectAnnotations.PROJECT_LABELS).split(",")))
                        .version(manifest.getAnnotations().get(ArtifactAnnotations.OCI_VERSION))
                        .ref(manifest.getAnnotations().get(ArtifactAnnotations.OCI_REF_NAME))
                        .build())
                .toList();
    }

    public static Path resolveCacheFileForCatalog(String catalogName) {
        Assert.hasText(catalogName, "catalogName cannot be empty");
        return resolveCacheDirectory().resolve(catalogName + ".json");
    }

    private static Path resolveCacheDirectory() {
        // 1. arconia.cache.home system property takes precedence on all platforms
        String arconiaCacheHome = System.getProperty("arconia.cache.home");
        if (arconiaCacheHome != null && !arconiaCacheHome.isBlank()) {
            return Path.of(arconiaCacheHome);
        }

        // 2. XDG_CACHE_HOME takes precedence on all platforms
        String xdgCacheHome = System.getenv("XDG_CACHE_HOME");
        if (xdgCacheHome != null && !xdgCacheHome.isBlank()) {
            return Path.of(xdgCacheHome, "arconia", "projects");
        }

        // 3. On Windows, use %LOCALAPPDATA% (typically C:\Users\<user>\AppData\Local)
        if (SystemUtils.isWindows()) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                return Path.of(localAppData, "arconia", "projects");
            }
        }

        // 4. Default: ~/.cache/arconia/projects (XDG standard for Linux/macOS)
        return Path.of(System.getProperty("user.home"), CACHE_SUBDIR);
    }

}
