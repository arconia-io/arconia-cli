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
 * Unit tests for {@link SkillCollectionCache}.
 */
class SkillCollectionCacheTests {

    @Test
    void cacheDirIsConfigured() {
        Path dir = SkillCollectionCache.cacheDir();

        assertThat(dir.toString()).contains("arconia");
        assertThat(dir.toString()).contains("skills");
    }

    @Test
    void cacheFileResolvesCorrectly() {
        Path file = SkillCollectionCache.cacheFile("my-collection");

        assertThat(file.getFileName().toString()).isEqualTo("my-collection.json");
        assertThat(file.getParent()).isEqualTo(SkillCollectionCache.cacheDir());
    }

    @Test
    void loadReturnsNullWhenNotCached() throws IOException {
        // Use a name that's very unlikely to exist
        assertThat(SkillCollectionCache.load("nonexistent-test-collection-xyz")).isNull();
    }

    @Test
    void saveAndLoadRoundTrip(@TempDir Path tempDir) throws IOException {
        // Manually save to a temp location and reload via Jackson
        Path cacheFile = tempDir.resolve("test-collection.json");

        SkillCollectionCache.CachedCollection collection = new SkillCollectionCache.CachedCollection(
            "test-collection",
            "1.0.0",
            "A test collection",
            List.of(
                new SkillCollectionCache.CachedSkill("skill-a", "1.2.0", "Skill A description",
                    "ghcr.io/org/skills/skill-a:1.2.0", "sha256:aaa"),
                new SkillCollectionCache.CachedSkill("skill-b", "2.0.1", "Skill B description",
                    "ghcr.io/org/skills/skill-b:2.0.1", "sha256:bbb")
            )
        );

        // Serialize and write manually
        var mapper = new tools.jackson.databind.json.JsonMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(collection);
        Files.writeString(cacheFile, json);

        // Read back
        String readJson = Files.readString(cacheFile);
        SkillCollectionCache.CachedCollection loaded = mapper.readValue(readJson, SkillCollectionCache.CachedCollection.class);

        assertThat(loaded.collectionName()).isEqualTo("test-collection");
        assertThat(loaded.collectionVersion()).isEqualTo("1.0.0");
        assertThat(loaded.description()).isEqualTo("A test collection");
        assertThat(loaded.skills()).hasSize(2);
        assertThat(loaded.skills().get(0).name()).isEqualTo("skill-a");
        assertThat(loaded.skills().get(0).ref()).isEqualTo("ghcr.io/org/skills/skill-a:1.2.0");
        assertThat(loaded.skills().get(1).name()).isEqualTo("skill-b");
    }

    @Test
    void isFreshReturnsTrueWhenWithinTtl() {
        String recentTimestamp = Instant.now().minusSeconds(60).toString();

        assertThat(SkillCollectionCache.isFresh(recentTimestamp, Duration.ofHours(1))).isTrue();
    }

    @Test
    void isFreshReturnsFalseWhenExpired() {
        String oldTimestamp = Instant.now().minus(Duration.ofHours(25)).toString();

        assertThat(SkillCollectionCache.isFresh(oldTimestamp, Duration.ofHours(24))).isFalse();
    }

    @Test
    void isFreshReturnsFalseForNullTimestamp() {
        assertThat(SkillCollectionCache.isFresh(null, Duration.ofHours(24))).isFalse();
    }

    @Test
    void isFreshReturnsFalseForBlankTimestamp() {
        assertThat(SkillCollectionCache.isFresh("", Duration.ofHours(24))).isFalse();
    }

    @Test
    void isFreshReturnsFalseForInvalidTimestamp() {
        assertThat(SkillCollectionCache.isFresh("not-a-timestamp", Duration.ofHours(24))).isFalse();
    }

    @Test
    void findSkillReturnsMatchingSkill() {
        SkillCollectionCache.CachedCollection collection = new SkillCollectionCache.CachedCollection(
            "test", null, null,
            List.of(
                new SkillCollectionCache.CachedSkill("alpha", "1.0", null, "ref-a", null),
                new SkillCollectionCache.CachedSkill("beta", "2.0", null, "ref-b", null)
            )
        );

        SkillCollectionCache.CachedSkill found = SkillCollectionCache.findSkill(collection, "beta");

        assertThat(found).isNotNull();
        assertThat(found.name()).isEqualTo("beta");
        assertThat(found.ref()).isEqualTo("ref-b");
    }

    @Test
    void findSkillReturnsNullWhenNotFound() {
        SkillCollectionCache.CachedCollection collection = new SkillCollectionCache.CachedCollection(
            "test", null, null,
            List.of(new SkillCollectionCache.CachedSkill("alpha", null, null, null, null))
        );

        assertThat(SkillCollectionCache.findSkill(collection, "nonexistent")).isNull();
    }

    @Test
    void cachedCollectionRequiresName() {
        assertThatThrownBy(() -> new SkillCollectionCache.CachedCollection("", null, null, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cachedSkillRequiresName() {
        assertThatThrownBy(() -> new SkillCollectionCache.CachedSkill("", null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromCollectionInfoConvertsCorrectly() {
        // Build a minimal OCI Index for the CollectionInfo record
        land.oras.Index index = land.oras.Index.fromJson("""
            {
              "schemaVersion": 2,
              "mediaType": "application/vnd.oci.image.index.v1+json",
              "manifests": []
            }
            """);

        SkillCollectionReader.CollectionInfo info = new SkillCollectionReader.CollectionInfo(
            "my-collection",
            "2.0",
            "My collection description",
            List.of(
                new SkillCollectionReader.CollectionSkillInfo("skill-x", "1.0.0", "Description X",
                    "ghcr.io/org/skill-x:1.0.0", "sha256:xxx")
            ),
            index
        );

        SkillCollectionCache.CachedCollection cached = SkillCollectionCache.fromCollectionInfo(info);

        assertThat(cached.collectionName()).isEqualTo("my-collection");
        assertThat(cached.collectionVersion()).isEqualTo("2.0");
        assertThat(cached.description()).isEqualTo("My collection description");
        assertThat(cached.skills()).hasSize(1);
        assertThat(cached.skills().getFirst().name()).isEqualTo("skill-x");
        assertThat(cached.skills().getFirst().ref()).isEqualTo("ghcr.io/org/skill-x:1.0.0");
    }

}
