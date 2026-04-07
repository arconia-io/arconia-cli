package io.arconia.cli.project.collection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.arconia.cli.project.collection.ProjectCollectionPushReport.ArtifactEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCollectionPushReport}.
 */
class ProjectCollectionPushReportTests {

    @TempDir
    Path tempDir;

    @Test
    void buildsSuccessfullyWithEmptyArtifacts() {
        ProjectCollectionPushReport report = ProjectCollectionPushReport.builder().build();

        assertThat(report.artifacts()).isEmpty();
    }

    @Test
    void buildsSuccessfullyWithArtifact() {
        ProjectCollectionPushReport report = ProjectCollectionPushReport.builder()
                .artifact(minimalEntry().build())
                .build();

        assertThat(report.artifacts()).hasSize(1);
    }

    @Test
    void buildsWithArtifactsList() {
        ProjectCollectionPushReport report = ProjectCollectionPushReport.builder()
                .artifacts(List.of(minimalEntry().build()))
                .build();

        assertThat(report.artifacts()).hasSize(1);
    }

    @Test
    void whenArtifactsIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCollectionPushReport(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifacts cannot be null");
    }

    @Test
    void whenArtifactAddedToBuilderIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionPushReport.builder().artifact(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifact cannot be null");
    }

    @Test
    void artifactEntryBuildsSuccessfully() {
        ArtifactEntry entry = minimalEntry().build();

        assertThat(entry.name()).isEqualTo("spring-templates");
        assertThat(entry.description()).isEqualTo("A collection of templates");
        assertThat(entry.ref()).isEqualTo("ghcr.io/org/collections/spring-templates");
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
    void whenEntryNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry().name("").build())
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
    void whenEntryDescriptionIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry().description("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description cannot be null or empty");
    }

    @Test
    void whenEntryRefIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().ref(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenEntryRefIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry().ref("").build())
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
    void whenEntryTagIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry().tag("").build())
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
    void whenEntryDigestIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry().digest("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digest cannot be null or empty");
    }

    @Test
    void saveWritesJsonFile() throws IOException {
        Path reportFile = tempDir.resolve("report.json");
        ProjectCollectionPushReport report = ProjectCollectionPushReport.builder()
                .artifact(minimalEntry().build())
                .build();

        report.save(reportFile);

        assertThat(reportFile).exists();
        String content = Files.readString(reportFile);
        assertThat(content).contains("spring-templates");
        assertThat(content).contains("sha256:abc123");
    }

    @Test
    void saveWritesNewlineAtEnd() throws IOException {
        Path reportFile = tempDir.resolve("report.json");

        ProjectCollectionPushReport.builder().build().save(reportFile);

        assertThat(Files.readString(reportFile)).endsWith("\n");
    }

    @Test
    void loadRoundTripsReport() throws IOException {
        Path reportFile = tempDir.resolve("report.json");
        ProjectCollectionPushReport original = ProjectCollectionPushReport.builder()
                .artifact(minimalEntry().build())
                .build();
        original.save(reportFile);

        ProjectCollectionPushReport loaded = ProjectCollectionPushReport.load(reportFile);

        assertThat(loaded.artifacts()).hasSize(1);
        ArtifactEntry entry = loaded.artifacts().getFirst();
        assertThat(entry.name()).isEqualTo("spring-templates");
        assertThat(entry.description()).isEqualTo("A collection of templates");
        assertThat(entry.ref()).isEqualTo("ghcr.io/org/collections/spring-templates");
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

        assertThat(ProjectCollectionPushReport.load(reportFile)).isNotNull();
    }

    @Test
    void saveThrowsWhenPathIsNull() {
        ProjectCollectionPushReport report = ProjectCollectionPushReport.builder().build();

        assertThatThrownBy(() -> report.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path cannot be null");
    }

    @Test
    void loadThrowsWhenPathIsNull() {
        assertThatThrownBy(() -> ProjectCollectionPushReport.load(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path cannot be null");
    }

    private static ArtifactEntry.Builder minimalEntry() {
        return ArtifactEntry.builder()
                .name("spring-templates")
                .description("A collection of templates")
                .ref("ghcr.io/org/collections/spring-templates")
                .tag("1.0.0")
                .digest("sha256:abc123");
    }

}
