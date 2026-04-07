package io.arconia.cli.project.collection;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCollectionEntry}.
 */
class ProjectCollectionEntryTests {

    @Test
    void buildsSuccessfullyWithAllFields() {
        ProjectCollectionEntry entry = minimalEntry().build();

        assertThat(entry.name()).isEqualTo("my-project");
        assertThat(entry.ref()).isEqualTo("ghcr.io/org/projects/my-project:1.0.0");
        assertThat(entry.tag()).isEqualTo("1.0.0");
        assertThat(entry.digest()).isEqualTo("sha256:abc123");
        assertThat(entry.description()).isEqualTo("A test project");
        assertThat(entry.type()).isEqualTo("service");
        assertThat(entry.labels()).containsExactly("java", "spring");
    }

    @Test
    void whenNameIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().name(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry().name("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenRefIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().ref(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenRefIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry().ref("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenTagIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().tag(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag cannot be null or empty");
    }

    @Test
    void whenTagIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry().tag("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag cannot be null or empty");
    }

    @Test
    void whenDigestIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().digest(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digest cannot be null or empty");
    }

    @Test
    void whenDigestIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry().digest("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digest cannot be null or empty");
    }

    @Test
    void whenDescriptionIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().description(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description cannot be null or empty");
    }

    @Test
    void whenDescriptionIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry().description("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description cannot be null or empty");
    }

    @Test
    void whenTypeIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().type(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type cannot be null or empty");
    }

    @Test
    void whenTypeIsEmptyThenThrow() {
        assertThatThrownBy(() -> minimalEntry().type("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type cannot be null or empty");
    }

    @Test
    void whenLabelsIsNullThenThrow() {
        assertThatThrownBy(() -> minimalEntry().labels(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("labels cannot be null");
    }

    @Test
    void emptyLabelsIsAllowed() {
        ProjectCollectionEntry entry = minimalEntry().labels(List.of()).build();

        assertThat(entry.labels()).isEmpty();
    }

    private static ProjectCollectionEntry.Builder minimalEntry() {
        return ProjectCollectionEntry.builder()
                .name("my-project")
                .ref("ghcr.io/org/projects/my-project:1.0.0")
                .tag("1.0.0")
                .digest("sha256:abc123")
                .description("A test project")
                .type("service")
                .labels(List.of("java", "spring"));
    }

}
