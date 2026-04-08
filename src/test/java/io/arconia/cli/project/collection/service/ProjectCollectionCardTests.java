package io.arconia.cli.project.collection.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCollectionCard}.
 */
class ProjectCollectionCardTests {

    @Test
    void buildsSuccessfullyWithAllFields() {
        ProjectCollectionCard card = minimalCard().build();

        assertThat(card.header()).isNotNull();
        assertThat(card.header().name()).isEqualTo("my-collection");
        assertThat(card.header().ref()).isEqualTo("ghcr.io/org/collections/my-collection:1.0.0");
        assertThat(card.header().description()).isEqualTo("A test collection");
        assertThat(card.summaries()).hasSize(1);
        assertThat(card.summaries().get(0).name()).isEqualTo("my-app");
    }

    @Test
    void buildsSuccessfullyWithEmptySummaries() {
        ProjectCollectionCard card = minimalCard().summaries(List.of()).build();

        assertThat(card.header()).isNotNull();
        assertThat(card.summaries()).isEmpty();
    }

    @Test
    void whenHeaderIsNullThenThrow() {
        assertThatThrownBy(() -> minimalCard().header(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("header cannot be null");
    }

    @Test
    void whenSummariesIsNullThenThrow() {
        assertThatThrownBy(() -> minimalCard().summaries(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("summaries cannot be null");
    }

    private static ProjectCollectionCard.Builder minimalCard() {
        ProjectCollectionHeader header = ProjectCollectionHeader.builder()
                .name("my-collection")
                .ref("ghcr.io/org/collections/my-collection:1.0.0")
                .description("A test collection")
                .build();
        ProjectCollectionSummary summary = ProjectCollectionSummary.builder()
                .name("my-app")
                .description("A test application")
                .type("service")
                .labels(List.of("java", "spring"))
                .version("1.0.0")
                .ref("ghcr.io/org/projects/my-app:1.0.0")
                .build();
        return ProjectCollectionCard.builder()
                .header(header)
                .summaries(List.of(summary));
    }

}
