package io.arconia.cli.project.collection.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCollectionSummary}.
 */
class ProjectCollectionSummaryTests {

    @Test
    void buildsSuccessfullyWithAllFields() {
        ProjectCollectionSummary summary = minimalSummary().build();

        assertThat(summary.name()).isEqualTo("my-app");
        assertThat(summary.description()).isEqualTo("A test application");
        assertThat(summary.type()).isEqualTo("service");
        assertThat(summary.labels()).containsExactly("java", "spring");
        assertThat(summary.version()).isEqualTo("1.0.0");
        assertThat(summary.ref()).isEqualTo("ghcr.io/org/projects/my-app:1.0.0");
    }

    @Test
    void whenNameIsNullThenThrow() {
        assertThatThrownBy(() -> minimalSummary().name(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalSummary().name("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenDescriptionIsNullThenThrow() {
        assertThatThrownBy(() -> minimalSummary().description(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description cannot be null or empty");
    }

    @Test
    void whenDescriptionIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalSummary().description("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description cannot be null or empty");
    }

    @Test
    void whenTypeIsNullThenThrow() {
        assertThatThrownBy(() -> minimalSummary().type(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type cannot be null or empty");
    }

    @Test
    void whenTypeIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalSummary().type("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type cannot be null or empty");
    }

    @Test
    void whenLabelsIsNullThenThrow() {
        assertThatThrownBy(() -> minimalSummary().labels(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("labels cannot be null");
    }

    @Test
    void whenVersionIsNullThenThrow() {
        assertThatThrownBy(() -> minimalSummary().version(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version cannot be null or empty");
    }

    @Test
    void whenVersionIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalSummary().version("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version cannot be null or empty");
    }

    @Test
    void whenRefIsNullThenThrow() {
        assertThatThrownBy(() -> minimalSummary().ref(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenRefIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalSummary().ref("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    private static ProjectCollectionSummary.Builder minimalSummary() {
        return ProjectCollectionSummary.builder()
                .name("my-app")
                .description("A test application")
                .type("service")
                .labels(List.of("java", "spring"))
                .version("1.0.0")
                .ref("ghcr.io/org/projects/my-app:1.0.0");
    }

}
