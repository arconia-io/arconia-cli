package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SkillCatalogCache}.
 */
class SkillCatalogCacheTests {

    @Test
    void cacheDirIsConfigured() {
        Path dir = SkillCatalogCache.cacheDir();

        assertThat(dir.toString()).contains("arconia");
        assertThat(dir.toString()).contains("skills");
    }

    @Test
    void cacheFileResolvesCorrectly() {
        Path file = SkillCatalogCache.cacheFile("my-catalog");

        assertThat(file.getFileName().toString()).isEqualTo("my-catalog.json");
        assertThat(file.getParent()).isEqualTo(SkillCatalogCache.cacheDir());
    }

    @Test
    void loadReturnsNullWhenNotCached() throws IOException {
        // Use a name that's very unlikely to exist
        assertThat(SkillCatalogCache.load("nonexistent-test-catalog-xyz")).isNull();
    }

    @Test
    void saveAndLoadRoundTrip(@TempDir Path tempDir) throws IOException {
        // Manually save to a temp location and reload via Jackson
        Path cacheFile = tempDir.resolve("test-catalog.json");

        SkillCatalogCache.CachedCatalog catalog = new SkillCatalogCache.CachedCatalog(
            "test-catalog",
            "1.0.0",
            "A test catalog",
            List.of(
                new SkillCatalogCache.CachedSkill("skill-a", "1.2.0", "Skill A description",
                    "ghcr.io/org/skills/skill-a:1.2.0", "sha256:aaa"),
                new SkillCatalogCache.CachedSkill("skill-b", "2.0.1", "Skill B description",
                    "ghcr.io/org/skills/skill-b:2.0.1", "sha256:bbb")
            )
        );

        // Serialize and write manually
        var mapper = new tools.jackson.databind.json.JsonMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(catalog);
        Files.writeString(cacheFile, json);

        // Read back
        String readJson = Files.readString(cacheFile);
        SkillCatalogCache.CachedCatalog loaded = mapper.readValue(readJson, SkillCatalogCache.CachedCatalog.class);

        assertThat(loaded.catalogName()).isEqualTo("test-catalog");
        assertThat(loaded.catalogVersion()).isEqualTo("1.0.0");
        assertThat(loaded.description()).isEqualTo("A test catalog");
        assertThat(loaded.skills()).hasSize(2);
        assertThat(loaded.skills().get(0).name()).isEqualTo("skill-a");
        assertThat(loaded.skills().get(0).ref()).isEqualTo("ghcr.io/org/skills/skill-a:1.2.0");
        assertThat(loaded.skills().get(1).name()).isEqualTo("skill-b");
    }

    @Test
    void isFreshReturnsTrueWhenWithinTtl() {
        String recentTimestamp = Instant.now().minusSeconds(60).toString();

        assertThat(SkillCatalogCache.isFresh(recentTimestamp, Duration.ofHours(1))).isTrue();
    }

    @Test
    void isFreshReturnsFalseWhenExpired() {
        String oldTimestamp = Instant.now().minus(Duration.ofHours(25)).toString();

        assertThat(SkillCatalogCache.isFresh(oldTimestamp, Duration.ofHours(24))).isFalse();
    }

    @Test
    void isFreshReturnsFalseForNullTimestamp() {
        assertThat(SkillCatalogCache.isFresh(null, Duration.ofHours(24))).isFalse();
    }

    @Test
    void isFreshReturnsFalseForBlankTimestamp() {
        assertThat(SkillCatalogCache.isFresh("", Duration.ofHours(24))).isFalse();
    }

    @Test
    void isFreshReturnsFalseForInvalidTimestamp() {
        assertThat(SkillCatalogCache.isFresh("not-a-timestamp", Duration.ofHours(24))).isFalse();
    }

    @Test
    void findSkillReturnsMatchingSkill() {
        SkillCatalogCache.CachedCatalog catalog = new SkillCatalogCache.CachedCatalog(
            "test", null, null,
            List.of(
                new SkillCatalogCache.CachedSkill("alpha", "1.0", null, "ref-a", null),
                new SkillCatalogCache.CachedSkill("beta", "2.0", null, "ref-b", null)
            )
        );

        SkillCatalogCache.CachedSkill found = SkillCatalogCache.findSkill(catalog, "beta");

        assertThat(found).isNotNull();
        assertThat(found.name()).isEqualTo("beta");
        assertThat(found.ref()).isEqualTo("ref-b");
    }

    @Test
    void findSkillReturnsNullWhenNotFound() {
        SkillCatalogCache.CachedCatalog catalog = new SkillCatalogCache.CachedCatalog(
            "test", null, null,
            List.of(new SkillCatalogCache.CachedSkill("alpha", null, null, null, null))
        );

        assertThat(SkillCatalogCache.findSkill(catalog, "nonexistent")).isNull();
    }

    @Test
    void cachedCatalogRequiresName() {
        assertThatThrownBy(() -> new SkillCatalogCache.CachedCatalog("", null, null, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cachedSkillRequiresName() {
        assertThatThrownBy(() -> new SkillCatalogCache.CachedSkill("", null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromCatalogInfoConvertsCorrectly() {
        // Build a minimal OCI Index for the CatalogInfo record
        land.oras.Index index = land.oras.Index.fromJson("""
            {
              "schemaVersion": 2,
              "mediaType": "application/vnd.oci.image.index.v1+json",
              "manifests": []
            }
            """);

        SkillCatalogReader.CatalogInfo info = new SkillCatalogReader.CatalogInfo(
            "my-catalog",
            "2.0",
            "My catalog description",
            List.of(
                new SkillCatalogReader.CatalogSkillInfo("skill-x", "1.0.0", "Description X",
                    "ghcr.io/org/skill-x:1.0.0", "sha256:xxx")
            ),
            index
        );

        SkillCatalogCache.CachedCatalog cached = SkillCatalogCache.fromCatalogInfo(info);

        assertThat(cached.catalogName()).isEqualTo("my-catalog");
        assertThat(cached.catalogVersion()).isEqualTo("2.0");
        assertThat(cached.description()).isEqualTo("My catalog description");
        assertThat(cached.skills()).hasSize(1);
        assertThat(cached.skills().getFirst().name()).isEqualTo("skill-x");
        assertThat(cached.skills().getFirst().ref()).isEqualTo("ghcr.io/org/skill-x:1.0.0");
    }

}
