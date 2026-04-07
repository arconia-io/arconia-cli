package io.arconia.cli.project.collection.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.arconia.cli.project.collection.service.ProjectCollectionRegistry.CollectionEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCollectionRegistry}.
 */
class ProjectCollectionRegistryTests {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        System.setProperty("arconia.config.home", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("arconia.config.home");
    }

    @Test
    void buildsSuccessfullyWithEmptyCollections() {
        ProjectCollectionRegistry registry = ProjectCollectionRegistry.builder().build();

        assertThat(registry.collections()).isEmpty();
    }

    @Test
    void buildsSuccessfullyWithCollections() {
        ProjectCollectionRegistry registry = ProjectCollectionRegistry.builder()
                .collections(List.of(minimalEntry("my-collection").build()))
                .build();

        assertThat(registry.collections()).hasSize(1);
    }

    @Test
    void whenCollectionsIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCollectionRegistry(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collections cannot be null");
    }

    @Test
    void collectionEntryBuildsSuccessfully() {
        CollectionEntry entry = minimalEntry("my-collection").build();

        assertThat(entry.name()).isEqualTo("my-collection");
        assertThat(entry.ref()).isEqualTo("ghcr.io/org/collections/my-collection");
        assertThat(entry.digest()).isEqualTo("sha256:abc123");
        assertThat(entry.lastFetchedAt()).isNotNull();
    }

    @Test
    void whenCollectionEntryNameIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-collection").name(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenCollectionEntryNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-collection").name("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenCollectionEntryRefIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-collection").ref(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenCollectionEntryRefIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-collection").ref("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenCollectionEntryDigestIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-collection").digest(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digest cannot be null or empty");
    }

    @Test
    void whenCollectionEntryDigestIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-collection").digest("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digest cannot be null or empty");
    }

    @Test
    void whenCollectionEntryLastFetchedAtIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-collection").lastFetchedAt(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lastFetchedAt cannot be null");
    }

    @Test
    void loadReturnsEmptyRegistryWhenFileDoesNotExist() throws IOException {
        ProjectCollectionRegistry registry = ProjectCollectionRegistry.load();

        assertThat(registry.collections()).isEmpty();
    }

    @Test
    void saveAndLoadRoundTrip() throws IOException {
        CollectionEntry entry = minimalEntry("my-collection").build();
        ProjectCollectionRegistry original = ProjectCollectionRegistry.builder()
                .collections(List.of(entry))
                .build();

        ProjectCollectionRegistry.save(original);
        ProjectCollectionRegistry loaded = ProjectCollectionRegistry.load();

        assertThat(loaded.collections()).hasSize(1);
        CollectionEntry loadedEntry = loaded.collections().getFirst();
        assertThat(loadedEntry.name()).isEqualTo("my-collection");
        assertThat(loadedEntry.ref()).isEqualTo("ghcr.io/org/collections/my-collection");
        assertThat(loadedEntry.digest()).isEqualTo("sha256:abc123");
        assertThat(loadedEntry.lastFetchedAt()).isNotNull();
    }

    @Test
    void addCollectionAddsEntry() throws IOException {
        ProjectCollectionRegistry.addCollection(minimalEntry("my-collection").build());

        ProjectCollectionRegistry loaded = ProjectCollectionRegistry.load();
        assertThat(loaded.collections()).hasSize(1);
        assertThat(loaded.collections().getFirst().name()).isEqualTo("my-collection");
    }

    @Test
    void addCollectionReplacesEntryWithSameName() throws IOException {
        ProjectCollectionRegistry.addCollection(minimalEntry("my-collection")
                .ref("ghcr.io/org/collections/old-ref").build());
        ProjectCollectionRegistry.addCollection(minimalEntry("my-collection")
                .ref("ghcr.io/org/collections/new-ref").build());

        ProjectCollectionRegistry loaded = ProjectCollectionRegistry.load();
        assertThat(loaded.collections()).hasSize(1);
        assertThat(loaded.collections().getFirst().ref()).isEqualTo("ghcr.io/org/collections/new-ref");
    }

    @Test
    void addCollectionPreservesOtherEntries() throws IOException {
        ProjectCollectionRegistry.addCollection(minimalEntry("collection-a").build());
        ProjectCollectionRegistry.addCollection(minimalEntry("collection-b").build());

        assertThat(ProjectCollectionRegistry.load().collections()).hasSize(2);
    }

    @Test
    void whenAddCollectionEntryIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionRegistry.addCollection(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entry cannot be null");
    }

    @Test
    void removeCollectionRemovesEntryByName() throws IOException {
        ProjectCollectionRegistry.addCollection(minimalEntry("collection-a").build());
        ProjectCollectionRegistry.addCollection(minimalEntry("collection-b").build());

        ProjectCollectionRegistry.removeCollection("collection-a");

        ProjectCollectionRegistry loaded = ProjectCollectionRegistry.load();
        assertThat(loaded.collections()).hasSize(1);
        assertThat(loaded.collections().getFirst().name()).isEqualTo("collection-b");
    }

    @Test
    void removeCollectionDoesNothingWhenNameNotFound() throws IOException {
        ProjectCollectionRegistry.addCollection(minimalEntry("my-collection").build());

        ProjectCollectionRegistry.removeCollection("nonexistent");

        assertThat(ProjectCollectionRegistry.load().collections()).hasSize(1);
    }

    @Test
    void whenRemoveCollectionNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionRegistry.removeCollection(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenRemoveCollectionNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionRegistry.removeCollection(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void findCollectionReturnsEntryWhenFound() throws IOException {
        ProjectCollectionRegistry.addCollection(minimalEntry("my-collection").build());

        CollectionEntry found = ProjectCollectionRegistry.findCollection("my-collection");

        assertThat(found).isNotNull();
        assertThat(found.name()).isEqualTo("my-collection");
    }

    @Test
    void findCollectionReturnsNullWhenNotFound() throws IOException {
        CollectionEntry found = ProjectCollectionRegistry.findCollection("nonexistent");

        assertThat(found).isNull();
    }

    @Test
    void whenFindCollectionNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionRegistry.findCollection(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenFindCollectionNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionRegistry.findCollection(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    private static CollectionEntry.Builder minimalEntry(String name) {
        return CollectionEntry.builder()
                .name(name)
                .ref("ghcr.io/org/collections/" + name)
                .digest("sha256:abc123")
                .lastFetchedAt(Instant.now());
    }

}
