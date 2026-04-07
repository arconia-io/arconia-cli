package io.arconia.cli.project.collection.oci;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.project.collection.ProjectCollectionPushArguments;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProjectCollectionAnnotations}.
 */
class ProjectCollectionAnnotationsTests {

    @TempDir
    Path nonGitDir;

    private final ProjectCollectionPushArguments arguments = ProjectCollectionPushArguments.builder()
            .ref("ghcr.io/org/collections/spring-templates")
            .tag("1.0.0")
            .name("spring-templates")
            .description("A collection of Spring templates")
            .annotations(Map.of())
            .build();

    @Test
    void computeAnnotationsIncludesDefinedAnnotations() {
        Map<String, String> annotations = ProjectCollectionAnnotations.computeAnnotations(nonGitDir, arguments);

        assertThat(annotations)
                .containsEntry(ArtifactAnnotations.OCI_TITLE, "spring-templates")
                .containsEntry(ArtifactAnnotations.OCI_DESCRIPTION, "A collection of Spring templates")
                .containsEntry(ArtifactAnnotations.OCI_VERSION, "1.0.0")
                .containsKey(ArtifactAnnotations.OCI_CREATED);

        assertThat(annotations)
                .doesNotContainKey(ArtifactAnnotations.OCI_SOURCE)
                .doesNotContainKey(ArtifactAnnotations.OCI_REVISION);
    }

    @Test
    void computeAnnotationsOciCreatedMatchesIso8601() {
        Map<String, String> annotations = ProjectCollectionAnnotations.computeAnnotations(nonGitDir, arguments);

        assertThat(annotations.get(ArtifactAnnotations.OCI_CREATED))
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
    }

    @Test
    void computeAnnotationsIncludesAdditionalAnnotations() {
        ProjectCollectionPushArguments argsWithExtra = ProjectCollectionPushArguments.builder()
                .ref("ghcr.io/org/collections/spring-templates")
                .tag("1.0.0")
                .name("spring-templates")
                .description("A collection of Spring templates")
                .annotations(Map.of("custom.key", "custom-value"))
                .build();

        Map<String, String> annotations = ProjectCollectionAnnotations.computeAnnotations(nonGitDir, argsWithExtra);

        assertThat(annotations).containsEntry("custom.key", "custom-value");
    }

    @Test
    void computeAnnotationsAdditionalAnnotationsOverrideExisting() {
        ProjectCollectionPushArguments argsWithOverride = ProjectCollectionPushArguments.builder()
                .ref("ghcr.io/org/collections/spring-templates")
                .tag("1.0.0")
                .name("spring-templates")
                .description("A collection of Spring templates")
                .annotations(Map.of(ArtifactAnnotations.OCI_TITLE, "overridden-title"))
                .build();

        Map<String, String> annotations = ProjectCollectionAnnotations.computeAnnotations(nonGitDir, argsWithOverride);

        assertThat(annotations).containsEntry(ArtifactAnnotations.OCI_TITLE, "overridden-title");
    }

    @Test
    void computeAnnotationsIncludesSourceAndRevisionInGitRepo() {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Map<String, String> annotations = ProjectCollectionAnnotations.computeAnnotations(projectRoot, arguments);

        assertThat(annotations.get(ArtifactAnnotations.OCI_SOURCE)).contains("arconia-cli");
        assertThat(annotations.get(ArtifactAnnotations.OCI_REVISION)).matches("[0-9a-f]{7,12}");
    }

    @Test
    void computeAnnotationsHandlesEmptyAdditionalAnnotations() {
        Map<String, String> annotations = ProjectCollectionAnnotations.computeAnnotations(nonGitDir, arguments);

        assertThat(annotations).isNotEmpty();
    }

}
