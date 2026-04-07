package io.arconia.cli.project;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectBatchPushArguments}.
 */
class ProjectBatchPushArgumentsTests {

    @Test
    void buildsSuccessfullyWithRequiredFields() {
        ProjectBatchPushArguments args = ProjectBatchPushArguments.builder()
                .baseRef("ghcr.io/org/projects")
                .tag("1.0.0")
                .annotations(Map.of())
                .projectPath(Path.of("/projects"))
                .build();

        assertThat(args.baseRef()).isEqualTo("ghcr.io/org/projects");
        assertThat(args.tag()).isEqualTo("1.0.0");
        assertThat(args.annotations()).isEmpty();
        assertThat(args.projectPath()).isEqualTo(Path.of("/projects"));
        assertThat(args.reportFileName()).isNull();
    }

    @Test
    void buildsSuccessfullyWithAllFields() {
        ProjectBatchPushArguments args = ProjectBatchPushArguments.builder()
                .baseRef("ghcr.io/org/projects")
                .tag("1.0.0")
                .annotations(Map.of("custom.key", "value"))
                .projectPath(Path.of("/projects"))
                .reportFileName("report.json")
                .build();

        assertThat(args.reportFileName()).isEqualTo("report.json");
        assertThat(args.annotations()).containsEntry("custom.key", "value");
    }

    @Test
    void whenBaseRefIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectBatchPushArguments.builder()
                .tag("1.0.0")
                .annotations(Map.of())
                .projectPath(Path.of("/projects"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseRef cannot be null or empty");
    }

    @Test
    void whenTagIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectBatchPushArguments.builder()
                .baseRef("ghcr.io/org/projects")
                .annotations(Map.of())
                .projectPath(Path.of("/projects"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag cannot be null or empty");
    }

    @Test
    void whenAnnotationsIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectBatchPushArguments.builder()
                .baseRef("ghcr.io/org/projects")
                .tag("1.0.0")
                .projectPath(Path.of("/projects"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("annotations cannot be null");
    }

    @Test
    void whenProjectPathIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectBatchPushArguments.builder()
                .baseRef("ghcr.io/org/projects")
                .tag("1.0.0")
                .annotations(Map.of())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectPath cannot be null");
    }

    @Test
    void reportFileNameIsOptional() {
        ProjectBatchPushArguments args = ProjectBatchPushArguments.builder()
                .baseRef("ghcr.io/org/projects")
                .tag("1.0.0")
                .annotations(Map.of())
                .projectPath(Path.of("/projects"))
                .build();

        assertThat(args.reportFileName()).isNull();
    }

}
