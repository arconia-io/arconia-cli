package io.arconia.cli.project.catalog;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCatalogPushArguments}.
 */
class ProjectCatalogPushArgumentsTests {

    @Test
    void buildsSuccessfullyWithRequiredFields() {
        ProjectCatalogPushArguments args = minimalArguments().build();

        assertThat(args.ref()).isEqualTo("ghcr.io/org/catalogs/spring-templates");
        assertThat(args.tag()).isEqualTo("1.0.0");
        assertThat(args.name()).isEqualTo("spring-templates");
        assertThat(args.description()).isEqualTo("A catalog of templates");
        assertThat(args.annotations()).isEmpty();
        assertThat(args.projects()).isNull();
        assertThat(args.fromReport()).isNull();
        assertThat(args.reportFileName()).isNull();
    }

    @Test
    void buildsSuccessfullyWithAllFields() {
        ProjectCatalogPushArguments args = minimalArguments()
                .projects(List.of("ghcr.io/org/projects/my-app:1.0.0"))
                .fromReport("/path/to/report.json")
                .reportFileName("catalog-push-report.json")
                .build();

        assertThat(args.projects()).containsExactly("ghcr.io/org/projects/my-app:1.0.0");
        assertThat(args.fromReport()).isEqualTo("/path/to/report.json");
        assertThat(args.reportFileName()).isEqualTo("catalog-push-report.json");
    }

    @Test
    void whenRefIsNullThenThrow() {
        assertThatThrownBy(() -> minimalArguments().ref(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenRefIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalArguments().ref("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenTagIsNullThenThrow() {
        assertThatThrownBy(() -> minimalArguments().tag(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag cannot be null or empty");
    }

    @Test
    void whenTagIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalArguments().tag("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag cannot be null or empty");
    }

    @Test
    void whenNameIsNullThenThrow() {
        assertThatThrownBy(() -> minimalArguments().name(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalArguments().name("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenDescriptionIsNullThenThrow() {
        assertThatThrownBy(() -> minimalArguments().description(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description cannot be null or empty");
    }

    @Test
    void whenDescriptionIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalArguments().description("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description cannot be null or empty");
    }

    @Test
    void whenAnnotationsIsNullThenThrow() {
        assertThatThrownBy(() -> minimalArguments().annotations(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("annotations cannot be null");
    }

    @Test
    void projectsIsOptional() {
        ProjectCatalogPushArguments args = minimalArguments().projects(null).build();

        assertThat(args.projects()).isNull();
    }

    @Test
    void fromReportIsOptional() {
        ProjectCatalogPushArguments args = minimalArguments().fromReport(null).build();

        assertThat(args.fromReport()).isNull();
    }

    @Test
    void reportFileNameIsOptional() {
        ProjectCatalogPushArguments args = minimalArguments().reportFileName(null).build();

        assertThat(args.reportFileName()).isNull();
    }

    private static ProjectCatalogPushArguments.Builder minimalArguments() {
        return ProjectCatalogPushArguments.builder()
                .ref("ghcr.io/org/catalogs/spring-templates")
                .tag("1.0.0")
                .name("spring-templates")
                .description("A catalog of templates")
                .annotations(Map.of());
    }

}
