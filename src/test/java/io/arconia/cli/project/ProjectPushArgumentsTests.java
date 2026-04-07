package io.arconia.cli.project;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectPushArguments}.
 */
class ProjectPushArgumentsTests {

    @Test
    void buildsSuccessfullyWithRequiredFields() {
        ProjectPushArguments args = ProjectPushArguments.builder()
                .ref("ghcr.io/org/projects/my-app")
                .tag("1.0.0")
                .annotations(Map.of())
                .projectPath(Path.of("/projects/my-app"))
                .build();

        assertThat(args.ref()).isEqualTo("ghcr.io/org/projects/my-app");
        assertThat(args.tag()).isEqualTo("1.0.0");
        assertThat(args.annotations()).isEmpty();
        assertThat(args.projectPath()).isEqualTo(Path.of("/projects/my-app"));
        assertThat(args.reportFileName()).isNull();
    }

    @Test
    void buildsSuccessfullyWithAllFields() {
        ProjectPushArguments args = ProjectPushArguments.builder()
                .ref("ghcr.io/org/projects/my-app")
                .tag("1.0.0")
                .annotations(Map.of("custom.key", "value"))
                .projectPath(Path.of("/projects/my-app"))
                .reportFileName("report.json")
                .build();

        assertThat(args.reportFileName()).isEqualTo("report.json");
        assertThat(args.annotations()).containsEntry("custom.key", "value");
    }

    @Test
    void whenRefIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectPushArguments.builder()
                .tag("1.0.0")
                .annotations(Map.of())
                .projectPath(Path.of("/projects/my-app"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenRefIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectPushArguments.builder()
                .ref("")
                .tag("1.0.0")
                .annotations(Map.of())
                .projectPath(Path.of("/projects/my-app"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ref cannot be null or empty");
    }

    @Test
    void whenTagIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectPushArguments.builder()
                .ref("ghcr.io/org/projects/my-app")
                .annotations(Map.of())
                .projectPath(Path.of("/projects/my-app"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag cannot be null or empty");
    }

    @Test
    void whenTagIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProjectPushArguments.builder()
                .ref("ghcr.io/org/projects/my-app")
                .tag("")
                .annotations(Map.of())
                .projectPath(Path.of("/projects/my-app"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag cannot be null or empty");
    }

    @Test
    void whenAnnotationsIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectPushArguments.builder()
                .ref("ghcr.io/org/projects/my-app")
                .tag("1.0.0")
                .projectPath(Path.of("/projects/my-app"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("annotations cannot be null");
    }

    @Test
    void whenProjectPathIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectPushArguments.builder()
                .ref("ghcr.io/org/projects/my-app")
                .tag("1.0.0")
                .annotations(Map.of())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectPath cannot be null");
    }

    @Test
    void reportFileNameIsOptional() {
        ProjectPushArguments args = ProjectPushArguments.builder()
                .ref("ghcr.io/org/projects/my-app")
                .tag("1.0.0")
                .annotations(Map.of())
                .projectPath(Path.of("/projects/my-app"))
                .build();

        assertThat(args.reportFileName()).isNull();
    }

}
