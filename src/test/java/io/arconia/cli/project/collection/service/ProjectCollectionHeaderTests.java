package io.arconia.cli.project.collection.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCollectionHeader}.
 */
class ProjectCollectionHeaderTests {

    @Test
    void buildsSuccessfullyWithRequiredFields() {
        ProjectCollectionHeader header = ProjectCollectionHeader.builder()
                .name("spring-templates")
                .ref("ghcr.io/org/collections/spring-templates")
                .build();

        assertThat(header.name()).isEqualTo("spring-templates");
        assertThat(header.ref()).isEqualTo("ghcr.io/org/collections/spring-templates");
        assertThat(header.description()).isNull();
    }

    @Test
    void buildsSuccessfullyWithAllFields() {
        ProjectCollectionHeader header = ProjectCollectionHeader.builder()
                .name("spring-templates")
                .ref("ghcr.io/org/collections/spring-templates")
                .description("A collection of Spring templates")
                .build();

        assertThat(header.description()).isEqualTo("A collection of Spring templates");
    }

    @Test
    void descriptionIsOptional() {
        ProjectCollectionHeader header = ProjectCollectionHeader.builder()
                .name("spring-templates")
                .ref("ghcr.io/org/collections/spring-templates")
                .description(null)
                .build();

        assertThat(header.description()).isNull();
    }

    @Test
    void whenNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionHeader.builder()
                .ref("ghcr.io/org/collections/spring-templates")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionHeader.builder()
                .name("")
                .ref("ghcr.io/org/collections/spring-templates")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenRefIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionHeader.builder()
                .name("spring-templates")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenRefIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectCollectionHeader.builder()
                .name("spring-templates")
                .ref("")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

}
