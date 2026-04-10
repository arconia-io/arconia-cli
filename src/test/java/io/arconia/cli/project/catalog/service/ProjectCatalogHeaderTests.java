package io.arconia.cli.project.catalog.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCatalogHeader}.
 */
class ProjectCatalogHeaderTests {

    @Test
    void buildsSuccessfullyWithRequiredFields() {
        ProjectCatalogHeader header = ProjectCatalogHeader.builder()
                .name("spring-templates")
                .ref("ghcr.io/org/catalogs/spring-templates")
                .build();

        assertThat(header.name()).isEqualTo("spring-templates");
        assertThat(header.ref()).isEqualTo("ghcr.io/org/catalogs/spring-templates");
        assertThat(header.description()).isNull();
    }

    @Test
    void buildsSuccessfullyWithAllFields() {
        ProjectCatalogHeader header = ProjectCatalogHeader.builder()
                .name("spring-templates")
                .ref("ghcr.io/org/catalogs/spring-templates")
                .description("A catalog of Spring templates")
                .build();

        assertThat(header.description()).isEqualTo("A catalog of Spring templates");
    }

    @Test
    void descriptionIsOptional() {
        ProjectCatalogHeader header = ProjectCatalogHeader.builder()
                .name("spring-templates")
                .ref("ghcr.io/org/catalogs/spring-templates")
                .description(null)
                .build();

        assertThat(header.description()).isNull();
    }

    @Test
    void whenNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogHeader.builder()
                .ref("ghcr.io/org/catalogs/spring-templates")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogHeader.builder()
                .name("")
                .ref("ghcr.io/org/catalogs/spring-templates")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenRefIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogHeader.builder()
                .name("spring-templates")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenRefIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCatalogHeader.builder()
                .name("spring-templates")
                .ref("")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

}
