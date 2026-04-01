package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SkillCatalogRegistry}.
 */
class SkillCatalogRegistryTests {

    @Test
    void builderCreatesRegistryWithCatalogs() {
        var entry = SkillCatalogRegistry.CatalogEntry.builder()
            .name("arconia")
            .ref("ghcr.io/arconia-io/skills-catalog:latest")
            .build();

        SkillCatalogRegistry registry = SkillCatalogRegistry.builder()
            .catalogs(java.util.List.of(entry))
            .build();

        assertThat(registry.catalogs()).hasSize(1);
        assertThat(registry.catalogs().getFirst().name()).isEqualTo("arconia");
        assertThat(registry.catalogs().getFirst().ref()).isEqualTo("ghcr.io/arconia-io/skills-catalog:latest");
        assertThat(registry.catalogs().getFirst().digest()).isNull();
        assertThat(registry.catalogs().getFirst().lastFetchedAt()).isNull();
    }

    @Test
    void builderCreatesEmptyRegistryByDefault() {
        SkillCatalogRegistry registry = SkillCatalogRegistry.builder().build();

        assertThat(registry.catalogs()).isEmpty();
    }

    @Test
    void mutateRegistryReturnsPrePopulatedBuilder() {
        var entry = SkillCatalogRegistry.CatalogEntry.builder()
            .name("original")
            .ref("ghcr.io/original:latest")
            .build();
        SkillCatalogRegistry original = SkillCatalogRegistry.builder()
            .catalogs(new java.util.ArrayList<>(java.util.List.of(entry)))
            .build();

        SkillCatalogRegistry modified = original.mutate()
            .catalogs(java.util.List.of(
                SkillCatalogRegistry.CatalogEntry.builder()
                    .name("modified")
                    .ref("ghcr.io/modified:latest")
                    .build()))
            .build();

        assertThat(modified.catalogs()).hasSize(1);
        assertThat(modified.catalogs().getFirst().name()).isEqualTo("modified");
    }

    @Test
    void catalogEntryBuilderCreatesEntryWithRequiredFields() {
        SkillCatalogRegistry.CatalogEntry entry = SkillCatalogRegistry.CatalogEntry.builder()
            .name("test")
            .ref("ghcr.io/test/catalog:latest")
            .build();

        assertThat(entry.name()).isEqualTo("test");
        assertThat(entry.ref()).isEqualTo("ghcr.io/test/catalog:latest");
        assertThat(entry.digest()).isNull();
        assertThat(entry.lastFetchedAt()).isNull();
    }

    @Test
    void catalogEntryBuilderCreatesEntryWithAllFields() {
        SkillCatalogRegistry.CatalogEntry entry = SkillCatalogRegistry.CatalogEntry.builder()
            .name("test")
            .ref("ghcr.io/test/catalog:latest")
            .digest("sha256:abc123")
            .lastFetchedAt("2026-01-01T00:00:00Z")
            .build();

        assertThat(entry.name()).isEqualTo("test");
        assertThat(entry.ref()).isEqualTo("ghcr.io/test/catalog:latest");
        assertThat(entry.digest()).isEqualTo("sha256:abc123");
        assertThat(entry.lastFetchedAt()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    void catalogEntryBuilderThrowsWhenNameIsMissing() {
        assertThatThrownBy(() -> SkillCatalogRegistry.CatalogEntry.builder()
                .ref("ghcr.io/test/catalog:latest")
                .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void catalogEntryBuilderThrowsWhenRefIsMissing() {
        assertThatThrownBy(() -> SkillCatalogRegistry.CatalogEntry.builder()
                .name("test")
                .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mutateCatalogEntryReturnsPrePopulatedBuilder() {
        SkillCatalogRegistry.CatalogEntry original = SkillCatalogRegistry.CatalogEntry.builder()
            .name("test")
            .ref("ghcr.io/test/catalog:1.0")
            .digest("sha256:abc123")
            .lastFetchedAt("2026-01-01T00:00:00Z")
            .build();

        SkillCatalogRegistry.CatalogEntry modified = original.mutate()
            .ref("ghcr.io/test/catalog:2.0")
            .build();

        assertThat(modified.name()).isEqualTo("test");
        assertThat(modified.ref()).isEqualTo("ghcr.io/test/catalog:2.0");
        assertThat(modified.digest()).isEqualTo("sha256:abc123");
        assertThat(modified.lastFetchedAt()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    void emptyRegistryHasNoCatalogs() {
        SkillCatalogRegistry registry = SkillCatalogRegistry.empty();

        assertThat(registry.catalogs()).isEmpty();
    }

    @Test
    void addCatalog() {
        SkillCatalogRegistry registry = SkillCatalogRegistry.empty();
        var entry = new SkillCatalogRegistry.CatalogEntry("test", "ghcr.io/test/catalog:latest", null, null);

        SkillCatalogRegistry updated = registry.addCatalog(entry);

        assertThat(updated.catalogs()).hasSize(1);
        assertThat(updated.catalogs().getFirst().name()).isEqualTo("test");
        assertThat(updated.catalogs().getFirst().ref()).isEqualTo("ghcr.io/test/catalog:latest");
    }

    @Test
    void addCatalogReplacesExisting() {
        SkillCatalogRegistry registry = SkillCatalogRegistry.empty()
            .addCatalog(new SkillCatalogRegistry.CatalogEntry("test", "ghcr.io/old/catalog:1.0", null, null));

        SkillCatalogRegistry updated = registry.addCatalog(
            new SkillCatalogRegistry.CatalogEntry("test", "ghcr.io/new/catalog:2.0", null, null));

        assertThat(updated.catalogs()).hasSize(1);
        assertThat(updated.catalogs().getFirst().ref()).isEqualTo("ghcr.io/new/catalog:2.0");
    }

    @Test
    void removeCatalog() {
        SkillCatalogRegistry registry = SkillCatalogRegistry.empty()
            .addCatalog(new SkillCatalogRegistry.CatalogEntry("a", "ghcr.io/a:latest", null, null))
            .addCatalog(new SkillCatalogRegistry.CatalogEntry("b", "ghcr.io/b:latest", null, null));

        SkillCatalogRegistry updated = registry.removeCatalog("a");

        assertThat(updated.catalogs()).hasSize(1);
        assertThat(updated.catalogs().getFirst().name()).isEqualTo("b");
    }

    @Test
    void removeCatalogThatDoesNotExist() {
        SkillCatalogRegistry registry = SkillCatalogRegistry.empty()
            .addCatalog(new SkillCatalogRegistry.CatalogEntry("a", "ghcr.io/a:latest", null, null));

        SkillCatalogRegistry updated = registry.removeCatalog("nonexistent");

        assertThat(updated.catalogs()).hasSize(1);
    }

    @Test
    void findCatalogByName() {
        SkillCatalogRegistry registry = SkillCatalogRegistry.empty()
            .addCatalog(new SkillCatalogRegistry.CatalogEntry("prod", "ghcr.io/prod:latest", null, null))
            .addCatalog(new SkillCatalogRegistry.CatalogEntry("dev", "ghcr.io/dev:latest", null, null));

        SkillCatalogRegistry.CatalogEntry found = registry.findCatalog("dev");

        assertThat(found).isNotNull();
        assertThat(found.ref()).isEqualTo("ghcr.io/dev:latest");
    }

    @Test
    void findCatalogReturnsNullWhenNotFound() {
        SkillCatalogRegistry registry = SkillCatalogRegistry.empty();

        assertThat(registry.findCatalog("missing")).isNull();
    }

    @Test
    void catalogEntryRequiresName() {
        assertThatThrownBy(() -> new SkillCatalogRegistry.CatalogEntry("", "ghcr.io/test:latest", null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void catalogEntryRequiresRef() {
        assertThatThrownBy(() -> new SkillCatalogRegistry.CatalogEntry("test", "", null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registryPathIsConfigured() {
        Path path = SkillCatalogRegistry.registryPath();

        assertThat(path.toString()).contains("arconia");
        assertThat(path.toString()).contains("skills");
        assertThat(path.getFileName().toString()).isEqualTo("catalogs.json");
    }

    @Test
    void saveAndLoadRoundTrip(@TempDir Path tempDir) throws IOException {
        // We can't easily test the real registryPath, but we can test
        // the serialization/deserialization through manual save/load
        Path registryFile = tempDir.resolve("catalogs.json");

        SkillCatalogRegistry registry = SkillCatalogRegistry.empty()
            .addCatalog(new SkillCatalogRegistry.CatalogEntry("arconia", "ghcr.io/arconia/catalog:latest", null, null))
            .addCatalog(new SkillCatalogRegistry.CatalogEntry("custom", "ghcr.io/custom/catalog:1.0", null, null));

        // Manually serialize and write
        String json = new tools.jackson.databind.json.JsonMapper().writerWithDefaultPrettyPrinter()
            .writeValueAsString(registry);
        Files.writeString(registryFile, json);

        // Read back and verify
        String readJson = Files.readString(registryFile);
        SkillCatalogRegistry loaded = new tools.jackson.databind.json.JsonMapper()
            .readValue(readJson, SkillCatalogRegistry.class);

        assertThat(loaded.catalogs()).hasSize(2);
        assertThat(loaded.findCatalog("arconia")).isNotNull();
        assertThat(loaded.findCatalog("arconia").ref()).isEqualTo("ghcr.io/arconia/catalog:latest");
        assertThat(loaded.findCatalog("custom")).isNotNull();
        assertThat(loaded.findCatalog("custom").ref()).isEqualTo("ghcr.io/custom/catalog:1.0");
    }

}
