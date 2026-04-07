package io.arconia.cli.project;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCreateArguments}.
 */
class ProjectCreateArgumentsTests {

    @Test
    void buildsSuccessfullyWithAllFields() {
        ProjectCreateArguments args = ProjectCreateArguments.builder()
                .name("my-app")
                .description("A test application")
                .group("io.example")
                .version("1.0.0")
                .packageName("io.example.myapp")
                .build();

        assertThat(args.name()).isEqualTo("my-app");
        assertThat(args.description()).isEqualTo("A test application");
        assertThat(args.group()).isEqualTo("io.example");
        assertThat(args.version()).isEqualTo("1.0.0");
        assertThat(args.packageName()).isEqualTo("io.example.myapp");
    }

    @Test
    void descriptionDefaultsToNameWhenNotProvided() {
        ProjectCreateArguments args = ProjectCreateArguments.builder()
                .name("my-app")
                .group("io.example")
                .version("0.0.1-SNAPSHOT")
                .build();

        assertThat(args.description()).isEqualTo("my-app");
    }

    @Test
    void descriptionDefaultsToNameWhenEmpty() {
        ProjectCreateArguments args = ProjectCreateArguments.builder()
                .name("my-app")
                .description("")
                .group("io.example")
                .version("0.0.1-SNAPSHOT")
                .build();

        assertThat(args.description()).isEqualTo("my-app");
    }

    @Test
    void packageNameDefaultsToGroupDotNameWhenNotProvided() {
        ProjectCreateArguments args = ProjectCreateArguments.builder()
                .name("my-app")
                .group("io.example")
                .version("0.0.1-SNAPSHOT")
                .build();

        assertThat(args.packageName()).isEqualTo("io.example.my-app");
    }

    @Test
    void packageNameDefaultsToGroupDotNameWhenEmpty() {
        ProjectCreateArguments args = ProjectCreateArguments.builder()
                .name("my-app")
                .group("io.example")
                .version("0.0.1-SNAPSHOT")
                .packageName("")
                .build();

        assertThat(args.packageName()).isEqualTo("io.example.my-app");
    }

    @Test
    void whenNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCreateArguments.builder()
                .description("A test application")
                .group("io.example")
                .version("0.0.1-SNAPSHOT")
                .packageName("io.example.myapp")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenVersionIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCreateArguments.builder()
                .name("my-app")
                .group("io.example")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version cannot be null or empty");
    }

    @Test
    void whenGroupIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectCreateArguments.builder()
                .name("my-app")
                .version("0.0.1-SNAPSHOT")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("group cannot be null or empty");
    }

}
