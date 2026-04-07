package io.arconia.cli.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.arconia.cli.project.ProjectPushReport.ArtifactEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectPushReport}.
 */
class ProjectPushReportTests {

    @TempDir
    Path tempDir;

    @Test
    void buildsSuccessfullyWithEmptyArtifacts() {
        ProjectPushReport report = ProjectPushReport.builder().build();

        assertThat(report.artifacts()).isEmpty();
    }

    @Test
    void buildsSuccessfullyWithArtifacts() {
        ProjectPushReport report = ProjectPushReport.builder()
                .artifact(minimalEntry().build())
                .build();

        assertThat(report.artifacts()).hasSize(1);
    }

    @Test
    void buildsWithArtifactsList() {
        ProjectPushReport report = ProjectPushReport.builder()
                .artifacts(List.of(minimalEntry().build()))
                .build();

        assertThat(report.artifacts()).hasSize(1);
    }

    @Test
    void whenArtifactsIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectPushReport(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifacts cannot be null");
    }

    @Test
    void whenArtifactAddedToBuilderIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectPushReport.builder().artifact(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifact cannot be null");
    }

    @Test
    void artifactEntryBuildsSuccessfully() {
        ArtifactEntry entry = minimalEntry().build();

        assertThat(entry.name()).isEqualTo("my-app");
        assertThat(entry.description()).isEqualTo("A test application");
        assertThat(entry.type()).isEqualTo("service");
        assertThat(entry.license()).isEqualTo("Apache-2.0");
        assertThat(entry.labels()).containsExactly("java");
        assertThat(entry.ref()).isEqualTo("ghcr.io/org/projects/my-app");
        assertThat(entry.tag()).isEqualTo("1.0.0");
        assertThat(entry.digest()).isEqualTo("sha256:abc123");
    }

    @Test
    void whenEntryNameIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().name(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenEntryDescriptionIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().description(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description cannot be null or empty");
    }

    @Test
    void whenEntryTypeIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().type(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type cannot be null or empty");
    }

    @Test
    void whenEntryLicenseIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().license(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("license cannot be null or empty");
    }

    @Test
    void whenEntryLabelsIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().labels(null).build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void whenEntryRefIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().ref(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenEntryTagIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().tag(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag cannot be null or empty");
    }

    @Test
    void whenEntryDigestIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().digest(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digest cannot be null or empty");
    }

    @Test
    void saveWritesJsonFile() throws IOException {
        Path reportFile = tempDir.resolve("report.json");
        ProjectPushReport report = ProjectPushReport.builder()
                .artifact(minimalEntry().build())
                .build();

        report.save(reportFile);

        assertThat(reportFile).exists();
        String content = Files.readString(reportFile);
        assertThat(content).contains("my-app");
        assertThat(content).contains("sha256:abc123");
    }

    @Test
    void saveWritesNewlineAtEnd() throws IOException {
        Path reportFile = tempDir.resolve("report.json");

        ProjectPushReport.builder().build().save(reportFile);

        assertThat(Files.readString(reportFile)).endsWith("\n");
    }

    @Test
    void loadRoundTripsReport() throws IOException {
        Path reportFile = tempDir.resolve("report.json");
        ProjectPushReport original = ProjectPushReport.builder()
                .artifact(minimalEntry().build())
                .build();
        original.save(reportFile);

        ProjectPushReport loaded = ProjectPushReport.load(reportFile);

        assertThat(loaded.artifacts()).hasSize(1);
        ArtifactEntry entry = loaded.artifacts().get(0);
        assertThat(entry.name()).isEqualTo("my-app");
        assertThat(entry.description()).isEqualTo("A test application");
        assertThat(entry.type()).isEqualTo("service");
        assertThat(entry.license()).isEqualTo("Apache-2.0");
        assertThat(entry.labels()).containsExactly("java");
        assertThat(entry.ref()).isEqualTo("ghcr.io/org/projects/my-app");
        assertThat(entry.tag()).isEqualTo("1.0.0");
        assertThat(entry.digest()).isEqualTo("sha256:abc123");
    }

    @Test
    void loadIgnoresUnknownFields() throws IOException {
        Path reportFile = tempDir.resolve("report.json");
        Files.writeString(reportFile, """
                {
                  "artifacts" : [],
                  "unknown" : "ignored"
                }
                """);

        assertThat(ProjectPushReport.load(reportFile)).isNotNull();
    }

    @Test
    void saveThrowsWhenPathIsNull() {
        ProjectPushReport report = ProjectPushReport.builder().build();

        assertThatThrownBy(() -> report.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path cannot be null");
    }

    @Test
    void loadThrowsWhenPathIsNull() {
        assertThatThrownBy(() -> ProjectPushReport.load(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path cannot be null");
    }

    private static ArtifactEntry.Builder minimalEntry() {
        return ArtifactEntry.builder()
                .name("my-app")
                .description("A test application")
                .type("service")
                .license("Apache-2.0")
                .labels(List.of("java"))
                .ref("ghcr.io/org/projects/my-app")
                .tag("1.0.0")
                .digest("sha256:abc123");
    }

}
