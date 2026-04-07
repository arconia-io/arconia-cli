package io.arconia.cli.project.collection.service;

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
 * Unit tests for {@link ProjectCollectionCache}.
 */
class ProjectCollectionCacheTests {

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
    void whenSaveCollectionNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionCache.save(null, emptyIndex()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void whenSaveCollectionNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionCache.save("", emptyIndex()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void whenSaveIndexIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionCache.save("my-collection", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("index cannot be null");
    }

    @Test
    void whenLoadCollectionNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionCache.load(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void whenLoadCollectionNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionCache.load(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void whenDeleteCollectionNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionCache.delete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void whenDeleteCollectionNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionCache.delete(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void whenResolveCacheFileCollectionNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionCache.resolveCacheFileForCollection(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be empty");
    }

    @Test
    void whenResolveCacheFileCollectionNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionCache.resolveCacheFileForCollection(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be empty");
    }

    @Test
    void loadReturnsNullWhenCacheFileDoesNotExist() throws IOException {
        Index loaded = ProjectCollectionCache.load("my-collection");

        assertThat(loaded).isNull();
    }

    @Test
    void saveCreatesFile() throws IOException {
        ProjectCollectionCache.save("my-collection", emptyIndex());

        assertThat(ProjectCollectionCache.resolveCacheFileForCollection("my-collection")).exists();
    }

    @Test
    void saveAndLoadRoundTrip() throws IOException {
        ProjectCollectionCache.save("my-collection", emptyIndex());

        Index loaded = ProjectCollectionCache.load("my-collection");

        assertThat(loaded).isNotNull();
    }

    @Test
    void resolveCacheFileForCollectionReturnsJsonFile() {
        Path path = ProjectCollectionCache.resolveCacheFileForCollection("my-collection");

        assertThat(path.getFileName().toString()).isEqualTo("my-collection.json");
    }

    @Test
    void deleteRemovesCacheFile() throws IOException {
        ProjectCollectionCache.save("my-collection", emptyIndex());

        ProjectCollectionCache.delete("my-collection");

        assertThat(ProjectCollectionCache.resolveCacheFileForCollection("my-collection")).doesNotExist();
    }

    @Test
    void deleteDoesNothingWhenFileDoesNotExist() throws IOException {
        ProjectCollectionCache.delete("nonexistent");

        assertThat(ProjectCollectionCache.resolveCacheFileForCollection("nonexistent")).doesNotExist();
    }

    private static Index emptyIndex() {
        return Index.fromManifests(List.of());
    }

}
