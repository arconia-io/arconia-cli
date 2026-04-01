package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ArtifactPublishReport}.
 */
class ArtifactPublishReportTests {

    @TempDir
    Path tempDir;

    @Test
    void defaultSkillsFilenameIsCorrect() {
        assertThat(ArtifactPublishReport.DEFAULT_SKILLS_FILENAME).isEqualTo("publish-skills-report.json");
    }

    @Test
    void defaultCatalogFilenameIsCorrect() {
        assertThat(ArtifactPublishReport.DEFAULT_CATALOG_FILENAME).isEqualTo("publish-skills-catalog-report.json");
    }

    @Test
    void emptyReportHasNoArtifacts() {
        ArtifactPublishReport report = ArtifactPublishReport.empty("2026-03-30T19:00:00Z");

        assertThat(report.publishedAt()).isEqualTo("2026-03-30T19:00:00Z");
        assertThat(report.artifacts()).isEmpty();
    }

    @Test
    void withArtifactAddsEntry() {
        ArtifactPublishReport report = ArtifactPublishReport.empty("2026-03-30T19:00:00Z");

        ArtifactPublishReport updated = report.withArtifact(new ArtifactPublishReport.ArtifactEntry(
            "pull-request",
            "ghcr.io/org/skills/pull-request:1.0.0",
            "1.0.0",
            "sha256:abc123",
            "Creates PRs"
        ));

        assertThat(updated.artifacts()).hasSize(1);
        assertThat(updated.artifacts().getFirst().name()).isEqualTo("pull-request");
        assertThat(updated.artifacts().getFirst().ref()).isEqualTo("ghcr.io/org/skills/pull-request:1.0.0");
        assertThat(updated.artifacts().getFirst().version()).isEqualTo("1.0.0");
        assertThat(updated.artifacts().getFirst().digest()).isEqualTo("sha256:abc123");
        assertThat(updated.artifacts().getFirst().description()).isEqualTo("Creates PRs");
        // Original is immutable
        assertThat(report.artifacts()).isEmpty();
    }

    @Test
    void withArtifactAllowsNullDescription() {
        ArtifactPublishReport report = ArtifactPublishReport.empty("2026-03-30T19:00:00Z");

        ArtifactPublishReport updated = report.withArtifact(new ArtifactPublishReport.ArtifactEntry(
            "upgrade", "ghcr.io/org/skills/upgrade:1.0.0", "1.0.0", "sha256:def456", null
        ));

        assertThat(updated.artifacts().getFirst().description()).isNull();
    }

    @Test
    void saveAndLoadRoundTrip() throws IOException {
        ArtifactPublishReport original = ArtifactPublishReport.empty("2026-03-30T19:00:00Z")
            .withArtifact(new ArtifactPublishReport.ArtifactEntry(
                "pull-request", "ghcr.io/org/skills/pull-request:1.2.0", "1.2.0", "sha256:aaa", "Creates PRs"))
            .withArtifact(new ArtifactPublishReport.ArtifactEntry(
                "unit-tests", "ghcr.io/org/skills/unit-tests:1.2.0", "1.2.0", "sha256:bbb", null));

        Path file = tempDir.resolve("report.json");
        original.save(file);

        assertThat(Files.exists(file)).isTrue();

        ArtifactPublishReport loaded = ArtifactPublishReport.load(file);

        assertThat(loaded.publishedAt()).isEqualTo("2026-03-30T19:00:00Z");
        assertThat(loaded.artifacts()).hasSize(2);
        assertThat(loaded.artifacts().get(0).name()).isEqualTo("pull-request");
        assertThat(loaded.artifacts().get(0).version()).isEqualTo("1.2.0");
        assertThat(loaded.artifacts().get(1).name()).isEqualTo("unit-tests");
        assertThat(loaded.artifacts().get(1).version()).isEqualTo("1.2.0");
        assertThat(loaded.artifacts().get(1).description()).isNull();
    }

    @Test
    void singleArtifactReportRoundTrip() throws IOException {
        ArtifactPublishReport original = ArtifactPublishReport.empty("2026-03-30T20:00:00Z")
            .withArtifact(new ArtifactPublishReport.ArtifactEntry(
                "pull-request", "ghcr.io/org/skills/pull-request:2.0.0", "2.0.0", "sha256:single", "A single skill"));

        Path file = tempDir.resolve("single-report.json");
        original.save(file);

        ArtifactPublishReport loaded = ArtifactPublishReport.load(file);

        assertThat(loaded.artifacts()).hasSize(1);
        assertThat(loaded.artifacts().getFirst().name()).isEqualTo("pull-request");
        assertThat(loaded.artifacts().getFirst().ref()).isEqualTo("ghcr.io/org/skills/pull-request:2.0.0");
        assertThat(loaded.artifacts().getFirst().version()).isEqualTo("2.0.0");
        assertThat(loaded.artifacts().getFirst().digest()).isEqualTo("sha256:single");
    }

    @Test
    void artifactEntryRequiresName() {
        assertThatThrownBy(() -> new ArtifactPublishReport.ArtifactEntry("", "ref", "1.0.0", "digest", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void artifactEntryRequiresRef() {
        assertThatThrownBy(() -> new ArtifactPublishReport.ArtifactEntry("name", "", "1.0.0", "digest", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void artifactEntryRequiresVersion() {
        assertThatThrownBy(() -> new ArtifactPublishReport.ArtifactEntry("name", "ref", "", "digest", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void artifactEntryRequiresDigest() {
        assertThatThrownBy(() -> new ArtifactPublishReport.ArtifactEntry("name", "ref", "1.0.0", "", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reportRequiresPublishedAt() {
        assertThatThrownBy(() -> new ArtifactPublishReport("", java.util.List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

}
