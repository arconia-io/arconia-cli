package io.arconia.cli.project.catalog.oci;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.project.catalog.ProjectCatalogPushArguments;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProjectCatalogAnnotations}.
 */
class ProjectCatalogAnnotationsTests {

    @TempDir
    Path nonGitDir;

    private final ProjectCatalogPushArguments arguments = ProjectCatalogPushArguments.builder()
            .ref("ghcr.io/org/catalogs/spring-templates")
            .tag("1.0.0")
            .name("spring-templates")
            .description("A catalog of Spring templates")
            .annotations(Map.of())
            .build();

    @Test
    void computeAnnotationsIncludesDefinedAnnotations() {
        Map<String, String> annotations = ProjectCatalogAnnotations.computeAnnotations(nonGitDir, arguments);

        assertThat(annotations)
                .containsEntry(ArtifactAnnotations.OCI_TITLE, "spring-templates")
                .containsEntry(ArtifactAnnotations.OCI_DESCRIPTION, "A catalog of Spring templates")
                .containsEntry(ArtifactAnnotations.OCI_VERSION, "1.0.0")
                .containsKey(ArtifactAnnotations.OCI_CREATED);

        assertThat(annotations)
                .doesNotContainKey(ArtifactAnnotations.OCI_SOURCE)
                .doesNotContainKey(ArtifactAnnotations.OCI_REVISION);
    }

    @Test
    void computeAnnotationsOciCreatedMatchesIso8601() {
        Map<String, String> annotations = ProjectCatalogAnnotations.computeAnnotations(nonGitDir, arguments);

        assertThat(annotations.get(ArtifactAnnotations.OCI_CREATED))
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
    }

    @Test
    void computeAnnotationsIncludesAdditionalAnnotations() {
        ProjectCatalogPushArguments argsWithExtra = ProjectCatalogPushArguments.builder()
                .ref("ghcr.io/org/catalogs/spring-templates")
                .tag("1.0.0")
                .name("spring-templates")
                .description("A catalog of Spring templates")
                .annotations(Map.of("custom.key", "custom-value"))
                .build();

        Map<String, String> annotations = ProjectCatalogAnnotations.computeAnnotations(nonGitDir, argsWithExtra);

        assertThat(annotations).containsEntry("custom.key", "custom-value");
    }

    @Test
    void computeAnnotationsAdditionalAnnotationsOverrideExisting() {
        ProjectCatalogPushArguments argsWithOverride = ProjectCatalogPushArguments.builder()
                .ref("ghcr.io/org/catalogs/spring-templates")
                .tag("1.0.0")
                .name("spring-templates")
                .description("A catalog of Spring templates")
                .annotations(Map.of(ArtifactAnnotations.OCI_TITLE, "overridden-title"))
                .build();

        Map<String, String> annotations = ProjectCatalogAnnotations.computeAnnotations(nonGitDir, argsWithOverride);

        assertThat(annotations).containsEntry(ArtifactAnnotations.OCI_TITLE, "overridden-title");
    }

    @Test
    void computeAnnotationsIncludesSourceAndRevisionInGitRepo() {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Map<String, String> annotations = ProjectCatalogAnnotations.computeAnnotations(projectRoot, arguments);

        assertThat(annotations.get(ArtifactAnnotations.OCI_SOURCE)).contains("arconia-cli");
        assertThat(annotations.get(ArtifactAnnotations.OCI_REVISION)).matches("[0-9a-f]{7,12}");
    }

    @Test
    void computeAnnotationsHandlesEmptyAdditionalAnnotations() {
        Map<String, String> annotations = ProjectCatalogAnnotations.computeAnnotations(nonGitDir, arguments);

        assertThat(annotations).isNotEmpty();
    }

}
