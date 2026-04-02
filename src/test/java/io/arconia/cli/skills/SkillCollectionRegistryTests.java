package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SkillCollectionRegistry}.
 */
class SkillCollectionRegistryTests {

    @Test
    void builderCreatesRegistryWithCollections() {
        var entry = SkillCollectionRegistry.CollectionEntry.builder()
            .name("arconia")
            .ref("ghcr.io/arconia-io/skills-collection:latest")
            .build();

        SkillCollectionRegistry registry = SkillCollectionRegistry.builder()
            .collections(java.util.List.of(entry))
            .build();

        assertThat(registry.collections()).hasSize(1);
        assertThat(registry.collections().getFirst().name()).isEqualTo("arconia");
        assertThat(registry.collections().getFirst().ref()).isEqualTo("ghcr.io/arconia-io/skills-collection:latest");
        assertThat(registry.collections().getFirst().digest()).isNull();
        assertThat(registry.collections().getFirst().lastFetchedAt()).isNull();
    }

    @Test
    void builderCreatesEmptyRegistryByDefault() {
        SkillCollectionRegistry registry = SkillCollectionRegistry.builder().build();

        assertThat(registry.collections()).isEmpty();
    }

    @Test
    void mutateRegistryReturnsPrePopulatedBuilder() {
        var entry = SkillCollectionRegistry.CollectionEntry.builder()
            .name("original")
            .ref("ghcr.io/original:latest")
            .build();
        SkillCollectionRegistry original = SkillCollectionRegistry.builder()
            .collections(new java.util.ArrayList<>(java.util.List.of(entry)))
            .build();

        SkillCollectionRegistry modified = original.mutate()
            .collections(java.util.List.of(
                SkillCollectionRegistry.CollectionEntry.builder()
                    .name("modified")
                    .ref("ghcr.io/modified:latest")
                    .build()))
            .build();

        assertThat(modified.collections()).hasSize(1);
        assertThat(modified.collections().getFirst().name()).isEqualTo("modified");
    }

    @Test
    void collectionEntryBuilderCreatesEntryWithRequiredFields() {
        SkillCollectionRegistry.CollectionEntry entry = SkillCollectionRegistry.CollectionEntry.builder()
            .name("test")
            .ref("ghcr.io/test/collection:latest")
            .build();

        assertThat(entry.name()).isEqualTo("test");
        assertThat(entry.ref()).isEqualTo("ghcr.io/test/collection:latest");
        assertThat(entry.digest()).isNull();
        assertThat(entry.lastFetchedAt()).isNull();
    }

    @Test
    void collectionEntryBuilderCreatesEntryWithAllFields() {
        SkillCollectionRegistry.CollectionEntry entry = SkillCollectionRegistry.CollectionEntry.builder()
            .name("test")
            .ref("ghcr.io/test/collection:latest")
            .digest("sha256:abc123")
            .lastFetchedAt("2026-01-01T00:00:00Z")
            .build();

        assertThat(entry.name()).isEqualTo("test");
        assertThat(entry.ref()).isEqualTo("ghcr.io/test/collection:latest");
        assertThat(entry.digest()).isEqualTo("sha256:abc123");
        assertThat(entry.lastFetchedAt()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    void collectionEntryBuilderThrowsWhenNameIsMissing() {
        assertThatThrownBy(() -> SkillCollectionRegistry.CollectionEntry.builder()
                .ref("ghcr.io/test/collection:latest")
                .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void collectionEntryBuilderThrowsWhenRefIsMissing() {
        assertThatThrownBy(() -> SkillCollectionRegistry.CollectionEntry.builder()
                .name("test")
                .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mutateCollectionEntryReturnsPrePopulatedBuilder() {
        SkillCollectionRegistry.CollectionEntry original = SkillCollectionRegistry.CollectionEntry.builder()
            .name("test")
            .ref("ghcr.io/test/collection:1.0")
            .digest("sha256:abc123")
            .lastFetchedAt("2026-01-01T00:00:00Z")
            .build();

        SkillCollectionRegistry.CollectionEntry modified = original.mutate()
            .ref("ghcr.io/test/collection:2.0")
            .build();

        assertThat(modified.name()).isEqualTo("test");
        assertThat(modified.ref()).isEqualTo("ghcr.io/test/collection:2.0");
        assertThat(modified.digest()).isEqualTo("sha256:abc123");
        assertThat(modified.lastFetchedAt()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    void emptyRegistryHasNoCollections() {
        SkillCollectionRegistry registry = SkillCollectionRegistry.empty();

        assertThat(registry.collections()).isEmpty();
    }

    @Test
    void addCollection() {
        SkillCollectionRegistry registry = SkillCollectionRegistry.empty();
        var entry = new SkillCollectionRegistry.CollectionEntry("test", "ghcr.io/test/collection:latest", null, null);

        SkillCollectionRegistry updated = registry.addCollection(entry);

        assertThat(updated.collections()).hasSize(1);
        assertThat(updated.collections().getFirst().name()).isEqualTo("test");
        assertThat(updated.collections().getFirst().ref()).isEqualTo("ghcr.io/test/collection:latest");
    }

    @Test
    void addCollectionReplacesExisting() {
        SkillCollectionRegistry registry = SkillCollectionRegistry.empty()
            .addCollection(new SkillCollectionRegistry.CollectionEntry("test", "ghcr.io/old/collection:1.0", null, null));

        SkillCollectionRegistry updated = registry.addCollection(
            new SkillCollectionRegistry.CollectionEntry("test", "ghcr.io/new/collection:2.0", null, null));

        assertThat(updated.collections()).hasSize(1);
        assertThat(updated.collections().getFirst().ref()).isEqualTo("ghcr.io/new/collection:2.0");
    }

    @Test
    void removeCollection() {
        SkillCollectionRegistry registry = SkillCollectionRegistry.empty()
            .addCollection(new SkillCollectionRegistry.CollectionEntry("a", "ghcr.io/a:latest", null, null))
            .addCollection(new SkillCollectionRegistry.CollectionEntry("b", "ghcr.io/b:latest", null, null));

        SkillCollectionRegistry updated = registry.removeCollection("a");

        assertThat(updated.collections()).hasSize(1);
        assertThat(updated.collections().getFirst().name()).isEqualTo("b");
    }

    @Test
    void removeCollectionThatDoesNotExist() {
        SkillCollectionRegistry registry = SkillCollectionRegistry.empty()
            .addCollection(new SkillCollectionRegistry.CollectionEntry("a", "ghcr.io/a:latest", null, null));

        SkillCollectionRegistry updated = registry.removeCollection("nonexistent");

        assertThat(updated.collections()).hasSize(1);
    }

    @Test
    void findCollectionByName() {
        SkillCollectionRegistry registry = SkillCollectionRegistry.empty()
            .addCollection(new SkillCollectionRegistry.CollectionEntry("prod", "ghcr.io/prod:latest", null, null))
            .addCollection(new SkillCollectionRegistry.CollectionEntry("dev", "ghcr.io/dev:latest", null, null));

        SkillCollectionRegistry.CollectionEntry found = registry.findCollection("dev");

        assertThat(found).isNotNull();
        assertThat(found.ref()).isEqualTo("ghcr.io/dev:latest");
    }

    @Test
    void findCollectionReturnsNullWhenNotFound() {
        SkillCollectionRegistry registry = SkillCollectionRegistry.empty();

        assertThat(registry.findCollection("missing")).isNull();
    }

    @Test
    void collectionEntryRequiresName() {
        assertThatThrownBy(() -> new SkillCollectionRegistry.CollectionEntry("", "ghcr.io/test:latest", null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void collectionEntryRequiresRef() {
        assertThatThrownBy(() -> new SkillCollectionRegistry.CollectionEntry("test", "", null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registryPathIsConfigured() {
        Path path = SkillCollectionRegistry.registryPath();

        assertThat(path.toString()).contains("arconia");
        assertThat(path.toString()).contains("skills");
        assertThat(path.getFileName().toString()).isEqualTo("collections.json");
    }

    @Test
    void saveAndLoadRoundTrip(@TempDir Path tempDir) throws IOException {
        // We can't easily test the real registryPath, but we can test
        // the serialization/deserialization through manual save/load
        Path registryFile = tempDir.resolve("collections.json");

        SkillCollectionRegistry registry = SkillCollectionRegistry.empty()
            .addCollection(new SkillCollectionRegistry.CollectionEntry("arconia", "ghcr.io/arconia/collection:latest", null, null))
            .addCollection(new SkillCollectionRegistry.CollectionEntry("custom", "ghcr.io/custom/collection:1.0", null, null));

        // Manually serialize and write
        String json = new tools.jackson.databind.json.JsonMapper().writerWithDefaultPrettyPrinter()
            .writeValueAsString(registry);
        Files.writeString(registryFile, json);

        // Read back and verify
        String readJson = Files.readString(registryFile);
        SkillCollectionRegistry loaded = new tools.jackson.databind.json.JsonMapper()
            .readValue(readJson, SkillCollectionRegistry.class);

        assertThat(loaded.collections()).hasSize(2);
        assertThat(loaded.findCollection("arconia")).isNotNull();
        assertThat(loaded.findCollection("arconia").ref()).isEqualTo("ghcr.io/arconia/collection:latest");
        assertThat(loaded.findCollection("custom")).isNotNull();
        assertThat(loaded.findCollection("custom").ref()).isEqualTo("ghcr.io/custom/collection:1.0");
    }

}
