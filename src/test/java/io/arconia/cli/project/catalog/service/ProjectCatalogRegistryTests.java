package io.arconia.cli.project.catalog.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.arconia.cli.project.catalog.service.ProjectCatalogRegistry.CatalogEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCatalogRegistry}.
 */
class ProjectCatalogRegistryTests {

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
    void buildsSuccessfullyWithEmptyCatalogs() {
        ProjectCatalogRegistry registry = ProjectCatalogRegistry.builder().build();

        assertThat(registry.catalogs()).isEmpty();
    }

    @Test
    void buildsSuccessfullyWithCatalogs() {
        ProjectCatalogRegistry registry = ProjectCatalogRegistry.builder()
                .catalogs(List.of(minimalEntry("my-catalog").build()))
                .build();

        assertThat(registry.catalogs()).hasSize(1);
    }

    @Test
    void whenCatalogsIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogRegistry.builder().catalogs(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogs cannot be null");
    }

    @Test
    void catalogEntryBuildsSuccessfully() {
        CatalogEntry entry = minimalEntry("my-catalog").build();

        assertThat(entry.name()).isEqualTo("my-catalog");
        assertThat(entry.ref()).isEqualTo("ghcr.io/org/catalogs/my-catalog");
        assertThat(entry.digest()).isEqualTo("sha256:abc123");
        assertThat(entry.lastFetchedAt()).isNotNull();
    }

    @Test
    void whenCatalogEntryNameIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-catalog").name(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenCatalogEntryNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-catalog").name("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenCatalogEntryRefIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-catalog").ref(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenCatalogEntryRefIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-catalog").ref("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenCatalogEntryDigestIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-catalog").digest(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digest cannot be null or empty");
    }

    @Test
    void whenCatalogEntryDigestIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-catalog").digest("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digest cannot be null or empty");
    }

    @Test
    void whenCatalogEntryLastFetchedAtIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry("my-catalog").lastFetchedAt(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lastFetchedAt cannot be null");
    }

    @Test
    void loadReturnsEmptyRegistryWhenFileDoesNotExist() throws IOException {
        ProjectCatalogRegistry registry = ProjectCatalogRegistry.load();

        assertThat(registry.catalogs()).isEmpty();
    }

    @Test
    void saveAndLoadRoundTrip() throws IOException {
        CatalogEntry entry = minimalEntry("my-catalog").build();
        ProjectCatalogRegistry original = ProjectCatalogRegistry.builder()
                .catalogs(List.of(entry))
                .build();

        ProjectCatalogRegistry.save(original);
        ProjectCatalogRegistry loaded = ProjectCatalogRegistry.load();

        assertThat(loaded.catalogs()).hasSize(1);
        CatalogEntry loadedEntry = loaded.catalogs().getFirst();
        assertThat(loadedEntry.name()).isEqualTo("my-catalog");
        assertThat(loadedEntry.ref()).isEqualTo("ghcr.io/org/catalogs/my-catalog");
        assertThat(loadedEntry.digest()).isEqualTo("sha256:abc123");
        assertThat(loadedEntry.lastFetchedAt()).isNotNull();
    }

    @Test
    void addCatalogAddsEntry() throws IOException {
        ProjectCatalogRegistry.addCatalog(minimalEntry("my-catalog").build());

        ProjectCatalogRegistry loaded = ProjectCatalogRegistry.load();
        assertThat(loaded.catalogs()).hasSize(1);
        assertThat(loaded.catalogs().getFirst().name()).isEqualTo("my-catalog");
    }

    @Test
    void addCatalogReplacesEntryWithSameName() throws IOException {
        ProjectCatalogRegistry.addCatalog(minimalEntry("my-catalog")
                .ref("ghcr.io/org/catalogs/old-ref").build());
        ProjectCatalogRegistry.addCatalog(minimalEntry("my-catalog")
                .ref("ghcr.io/org/catalogs/new-ref").build());

        ProjectCatalogRegistry loaded = ProjectCatalogRegistry.load();
        assertThat(loaded.catalogs()).hasSize(1);
        assertThat(loaded.catalogs().getFirst().ref()).isEqualTo("ghcr.io/org/catalogs/new-ref");
    }

    @Test
    void addCatalogPreservesOtherEntries() throws IOException {
        ProjectCatalogRegistry.addCatalog(minimalEntry("catalog-a").build());
        ProjectCatalogRegistry.addCatalog(minimalEntry("catalog-b").build());

        assertThat(ProjectCatalogRegistry.load().catalogs()).hasSize(2);
    }

    @Test
    void whenAddCatalogEntryIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogRegistry.addCatalog(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entry cannot be null");
    }

    @Test
    void removeCatalogRemovesEntryByName() throws IOException {
        ProjectCatalogRegistry.addCatalog(minimalEntry("catalog-a").build());
        ProjectCatalogRegistry.addCatalog(minimalEntry("catalog-b").build());

        ProjectCatalogRegistry.removeCatalog("catalog-a");

        ProjectCatalogRegistry loaded = ProjectCatalogRegistry.load();
        assertThat(loaded.catalogs()).hasSize(1);
        assertThat(loaded.catalogs().getFirst().name()).isEqualTo("catalog-b");
    }

    @Test
    void removeCatalogDoesNothingWhenNameNotFound() throws IOException {
        ProjectCatalogRegistry.addCatalog(minimalEntry("my-catalog").build());

        ProjectCatalogRegistry.removeCatalog("nonexistent");

        assertThat(ProjectCatalogRegistry.load().catalogs()).hasSize(1);
    }

    @Test
    void whenRemoveCatalogNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogRegistry.removeCatalog(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenRemoveCatalogNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogRegistry.removeCatalog(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void findCatalogReturnsEntryWhenFound() throws IOException {
        ProjectCatalogRegistry.addCatalog(minimalEntry("my-catalog").build());

        CatalogEntry found = ProjectCatalogRegistry.findCatalog("my-catalog");

        assertThat(found).isNotNull();
        assertThat(found.name()).isEqualTo("my-catalog");
    }

    @Test
    void findCatalogReturnsNullWhenNotFound() throws IOException {
        CatalogEntry found = ProjectCatalogRegistry.findCatalog("nonexistent");

        assertThat(found).isNull();
    }

    @Test
    void whenFindCatalogNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogRegistry.findCatalog(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenFindCatalogNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogRegistry.findCatalog(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void builtInCatalogDismissedDefaultsToFalse() {
        ProjectCatalogRegistry registry = ProjectCatalogRegistry.builder().build();

        assertThat(registry.builtInCatalogDismissed()).isFalse();
    }

    @Test
    void builtInCatalogDismissedCanBeSetToTrue() {
        ProjectCatalogRegistry registry = ProjectCatalogRegistry.builder()
                .builtInCatalogDismissed(true)
                .build();

        assertThat(registry.builtInCatalogDismissed()).isTrue();
    }

    @Test
    void saveAndLoadRoundTripPreservesBuiltInCatalogDismissed() throws IOException {
        ProjectCatalogRegistry original = ProjectCatalogRegistry.builder()
                .builtInCatalogDismissed(true)
                .build();

        ProjectCatalogRegistry.save(original);
        ProjectCatalogRegistry loaded = ProjectCatalogRegistry.load();

        assertThat(loaded.builtInCatalogDismissed()).isTrue();
    }

    @Test
    void mutateCreatesBuilderWithCopiedFields() {
        ProjectCatalogRegistry registry = ProjectCatalogRegistry.builder()
                .catalogs(List.of(minimalEntry("my-catalog").build()))
                .builtInCatalogDismissed(true)
                .build();

        ProjectCatalogRegistry mutated = registry.mutate().build();

        assertThat(mutated.catalogs()).hasSize(1);
        assertThat(mutated.catalogs().getFirst().name()).isEqualTo("my-catalog");
        assertThat(mutated.builtInCatalogDismissed()).isTrue();
    }

    @Test
    void mutateAllowsOverridingFields() {
        ProjectCatalogRegistry registry = ProjectCatalogRegistry.builder()
                .builtInCatalogDismissed(true)
                .build();

        ProjectCatalogRegistry mutated = registry.mutate()
                .builtInCatalogDismissed(false)
                .build();

        assertThat(mutated.builtInCatalogDismissed()).isFalse();
    }

    @Test
    void addCatalogPreservesBuiltInCatalogDismissedFlag() throws IOException {
        ProjectCatalogRegistry registry = ProjectCatalogRegistry.builder()
                .builtInCatalogDismissed(true)
                .build();
        ProjectCatalogRegistry.save(registry);

        ProjectCatalogRegistry.addCatalog(minimalEntry("my-catalog").build());

        ProjectCatalogRegistry loaded = ProjectCatalogRegistry.load();
        assertThat(loaded.catalogs()).hasSize(1);
        assertThat(loaded.builtInCatalogDismissed()).isTrue();
    }

    @Test
    void loadDefaultsBuiltInCatalogDismissedToFalseWhenMissingFromJson() throws IOException {
        String json = """
                {
                  "catalogs": []
                }
                """;
        Files.writeString(ProjectCatalogRegistry.registryPath(), json);

        ProjectCatalogRegistry loaded = ProjectCatalogRegistry.load();

        assertThat(loaded.builtInCatalogDismissed()).isFalse();
    }

    @Test
    void removeCatalogPreservesBuiltInCatalogDismissedFlag() throws IOException {
        ProjectCatalogRegistry registry = ProjectCatalogRegistry.builder()
                .catalogs(List.of(minimalEntry("my-catalog").build()))
                .builtInCatalogDismissed(true)
                .build();
        ProjectCatalogRegistry.save(registry);

        ProjectCatalogRegistry.removeCatalog("my-catalog");

        ProjectCatalogRegistry loaded = ProjectCatalogRegistry.load();
        assertThat(loaded.catalogs()).isEmpty();
        assertThat(loaded.builtInCatalogDismissed()).isTrue();
    }

    private static CatalogEntry.Builder minimalEntry(String name) {
        return CatalogEntry.builder()
                .name(name)
                .ref("ghcr.io/org/catalogs/" + name)
                .digest("sha256:abc123")
                .lastFetchedAt(Instant.now());
    }

}
