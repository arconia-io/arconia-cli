package io.arconia.cli.project.oci;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectConfig}.
 */
class ProjectConfigTests {

    @Test
    void buildsSuccessfullyWithRequiredFields() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .build();

        assertThat(config.schemaVersion()).isEqualTo(ProjectConfig.CURRENT_SCHEMA_VERSION);
        assertThat(config.name()).isEqualTo("my-app");
        assertThat(config.description()).isEqualTo("A test application");
        assertThat(config.type()).isEqualTo(ProjectConfig.DEFAULT_TYPE);
        assertThat(config.license()).isEqualTo(ProjectConfig.DEFAULT_LICENSE);
        assertThat(config.packageName()).isEqualTo(ProjectConfig.DEFAULT_PACKAGE_NAME);
        assertThat(config.labels()).isEmpty();
    }

    @Test
    void buildsSuccessfullyWithAllFields() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .type("service")
                .license("Apache-2.0")
                .packageName("io.example.my-app")
                .labels(List.of("java", "spring"))
                .build();

        assertThat(config.type()).isEqualTo("service");
        assertThat(config.license()).isEqualTo("Apache-2.0");
        assertThat(config.packageName()).isEqualTo("io.example.my-app");
        assertThat(config.labels()).containsExactly("java", "spring");
    }

    @Test
    void whenNameIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectConfig.builder()
                .description("A test application")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenDescriptionIsNullThenThrow() {
        assertThatThrownBy(() -> ProjectConfig.builder()
                .name("my-app")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description cannot be null or empty");
    }

    @Test
    void whenPackageNameIsNullConstructorUsesDefault() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .packageName(null)
                .build();

        assertThat(config.packageName()).isEqualTo(ProjectConfig.DEFAULT_PACKAGE_NAME);
    }

    @Test
    void whenPackageNameIsEmptyConstructorUsesDefault() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .packageName("")
                .build();

        assertThat(config.packageName()).isEqualTo(ProjectConfig.DEFAULT_PACKAGE_NAME);
    }

    @Test
    void whenTypeIsNullConstructorUsesDefault() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .type(null)
                .build();

        assertThat(config.type()).isEqualTo(ProjectConfig.DEFAULT_TYPE);
    }

    @Test
    void whenTypeIsEmptyConstructorUsesDefault() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .type("")
                .build();

        assertThat(config.type()).isEqualTo(ProjectConfig.DEFAULT_TYPE);
    }

    @Test
    void typeIsStripped() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .type("  service  ")
                .build();

        assertThat(config.type()).isEqualTo("service");
    }

    @Test
    void whenLicenseIsNullConstructorUsesDefault() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .license(null)
                .build();

        assertThat(config.license()).isEqualTo(ProjectConfig.DEFAULT_LICENSE);
    }

    @Test
    void whenLicenseIsEmptyConstructorUsesDefault() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .license("")
                .build();

        assertThat(config.license()).isEqualTo(ProjectConfig.DEFAULT_LICENSE);
    }

    @Test
    void licenseIsStripped() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .license("  Apache-2.0  ")
                .build();

        assertThat(config.license()).isEqualTo("Apache-2.0");
    }

    @Test
    void whenLabelsIsNullConstructorUsesEmptyList() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .labels(null)
                .build();

        assertThat(config.labels()).isEmpty();
    }

    @Test
    void whenLabelsIsEmptyConstructorUsesEmptyList() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .labels(List.of())
                .build();

        assertThat(config.labels()).isEmpty();
    }

    @Test
    void labelsListIsImmutable() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .labels(List.of("java"))
                .build();

        assertThatThrownBy(() -> config.labels().add("spring"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void schemaVersionIsAlwaysCurrentVersion() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .build();

        assertThat(config.schemaVersion()).isEqualTo(ProjectConfig.CURRENT_SCHEMA_VERSION);
    }

}
