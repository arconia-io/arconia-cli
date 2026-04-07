package io.arconia.cli.project.oci;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.arconia.cli.artifact.ArtifactAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProjectAnnotations}.
 */
class ProjectAnnotationsTests {

    @TempDir
    Path nonGitDir;

    private final ProjectConfig config = ProjectConfig.builder()
            .name("my-app")
            .description("A test application")
            .type("service")
            .license("Apache-2.0")
            .build();

    @Test
    void computeAnnotationsIncludesDefinedAnnotations() {
        Map<String, String> annotations = ProjectAnnotations.computeAnnotations(config, nonGitDir, "1.0.0", Map.of());

        assertThat(annotations)
                .containsEntry(ArtifactAnnotations.OCI_TITLE, "my-app")
                .containsEntry(ArtifactAnnotations.OCI_DESCRIPTION, "A test application")
                .containsEntry(ArtifactAnnotations.OCI_VERSION, "1.0.0")
                .containsEntry(ArtifactAnnotations.OCI_LICENSES, "Apache-2.0")
                .containsKey(ArtifactAnnotations.OCI_CREATED)
                .containsEntry(ProjectAnnotations.PROJECT_TYPE, "service")
                .containsEntry(ProjectAnnotations.PROJECT_LABELS, "");

        assertThat(annotations)
                .doesNotContainKey(ArtifactAnnotations.OCI_SOURCE)
                .doesNotContainKey(ArtifactAnnotations.OCI_REVISION);
    }

    @Test
    void computeAnnotationsOciCreatedMatchesIso8601() {
        Map<String, String> annotations = ProjectAnnotations.computeAnnotations(config, nonGitDir, "1.0.0", Map.of());

        assertThat(annotations.get(ArtifactAnnotations.OCI_CREATED))
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
    }

    @Test
    void computeAnnotationsIncludesAdditionalAnnotations() {
        Map<String, String> annotations = ProjectAnnotations.computeAnnotations(config, nonGitDir, "1.0.0",
                Map.of("custom.key", "custom-value"));

        assertThat(annotations).containsEntry("custom.key", "custom-value");
    }

    @Test
    void computeAnnotationsAdditionalAnnotationsOverrideExisting() {
        Map<String, String> annotations = ProjectAnnotations.computeAnnotations(config, nonGitDir, "1.0.0",
                Map.of(ArtifactAnnotations.OCI_TITLE, "overridden-title"));

        assertThat(annotations).containsEntry(ArtifactAnnotations.OCI_TITLE, "overridden-title");
    }

    @Test
    void computeAnnotationsIncludesSourceAndRevisionInGitRepo() {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Map<String, String> annotations = ProjectAnnotations.computeAnnotations(config, projectRoot, "1.0.0", Map.of());

        assertThat(annotations.get(ArtifactAnnotations.OCI_SOURCE)).contains("arconia-cli");
        assertThat(annotations.get(ArtifactAnnotations.OCI_REVISION)).matches("[0-9a-f]{7,12}");
    }

    @Test
    void computeAnnotationsIncludesProjectLabels() {
        ProjectConfig configWithLabels = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .labels(List.of("java", "spring"))
                .build();

        Map<String, String> annotations = ProjectAnnotations.computeAnnotations(configWithLabels, nonGitDir, "1.0.0", Map.of());

        assertThat(annotations).containsEntry(ProjectAnnotations.PROJECT_LABELS, "java,spring");
    }

    @Test
    void computeAnnotationsHandlesNullAdditionalAnnotations() {
        Map<String, String> annotations = ProjectAnnotations.computeAnnotations(config, nonGitDir, "1.0.0", null);

        assertThat(annotations).isNotEmpty();
    }

    @Test
    void computeAnnotationsHandlesEmptyAdditionalAnnotations() {
        Map<String, String> annotations = ProjectAnnotations.computeAnnotations(config, nonGitDir, "1.0.0", Map.of());

        assertThat(annotations).isNotEmpty();
    }

}
