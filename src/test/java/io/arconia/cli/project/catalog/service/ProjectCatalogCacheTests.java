package io.arconia.cli.project.catalog.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import land.oras.Index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCatalogCache}.
 */
class ProjectCatalogCacheTests {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        System.setProperty("arconia.cache.home", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("arconia.cache.home");
    }

    @Test
    void whenSaveCatalogNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogCache.save(null, emptyIndex()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void whenSaveCatalogNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogCache.save("", emptyIndex()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void whenSaveIndexIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogCache.save("my-catalog", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("index cannot be null");
    }

    @Test
    void whenLoadCatalogNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogCache.load(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void whenLoadCatalogNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogCache.load(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void whenDeleteCatalogNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogCache.delete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void whenDeleteCatalogNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogCache.delete(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void whenResolveCacheFileCatalogNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogCache.resolveCacheFileForCatalog(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be empty");
    }

    @Test
    void whenResolveCacheFileCatalogNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogCache.resolveCacheFileForCatalog(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be empty");
    }

    @Test
    void loadReturnsNullWhenCacheFileDoesNotExist() throws IOException {
        Index loaded = ProjectCatalogCache.load("my-catalog");

        assertThat(loaded).isNull();
    }

    @Test
    void saveCreatesFile() throws IOException {
        ProjectCatalogCache.save("my-catalog", emptyIndex());

        assertThat(ProjectCatalogCache.resolveCacheFileForCatalog("my-catalog")).exists();
    }

    @Test
    void saveAndLoadRoundTrip() throws IOException {
        ProjectCatalogCache.save("my-catalog", emptyIndex());

        Index loaded = ProjectCatalogCache.load("my-catalog");

        assertThat(loaded).isNotNull();
    }

    @Test
    void resolveCacheFileForCatalogReturnsJsonFile() {
        Path path = ProjectCatalogCache.resolveCacheFileForCatalog("my-catalog");

        assertThat(path.getFileName().toString()).isEqualTo("my-catalog.json");
    }

    @Test
    void deleteRemovesCacheFile() throws IOException {
        ProjectCatalogCache.save("my-catalog", emptyIndex());

        ProjectCatalogCache.delete("my-catalog");

        assertThat(ProjectCatalogCache.resolveCacheFileForCatalog("my-catalog")).doesNotExist();
    }

    @Test
    void deleteDoesNothingWhenFileDoesNotExist() throws IOException {
        ProjectCatalogCache.delete("nonexistent");

        assertThat(ProjectCatalogCache.resolveCacheFileForCatalog("nonexistent")).doesNotExist();
    }

    private static Index emptyIndex() {
        return Index.fromManifests(List.of());
    }

}
