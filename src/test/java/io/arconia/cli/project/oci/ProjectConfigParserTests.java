package io.arconia.cli.project.oci;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectConfigParser}.
 */
class ProjectConfigParserTests {

    @TempDir
    Path tempDir;

    @Test
    void parseFromDirectoryReturnsProjectConfig() throws IOException {
        Files.writeString(tempDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), """
                schemaVersion: "1"
                name: my-app
                description: A test application
                type: service
                license: Apache-2.0
                packageName: io.example.my-app
                labels: []
                """);

        ProjectConfig config = ProjectConfigParser.parseFromDirectory(tempDir);

        assertThat(config.name()).isEqualTo("my-app");
        assertThat(config.description()).isEqualTo("A test application");
        assertThat(config.type()).isEqualTo("service");
        assertThat(config.license()).isEqualTo("Apache-2.0");
        assertThat(config.packageName()).isEqualTo("io.example.my-app");
        assertThat(config.labels()).isEmpty();
        assertThat(config.schemaVersion()).isEqualTo(ProjectConfig.CURRENT_SCHEMA_VERSION);
    }

    @Test
    void parseFromDirectoryUsesDefaultsForOptionalFields() throws IOException {
        Files.writeString(tempDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), """
                schemaVersion: "1"
                name: my-app
                description: A test application
                """);

        ProjectConfig config = ProjectConfigParser.parseFromDirectory(tempDir);

        assertThat(config.schemaVersion()).isEqualTo(ProjectConfig.CURRENT_SCHEMA_VERSION);
        assertThat(config.type()).isEqualTo(ProjectConfig.DEFAULT_TYPE);
        assertThat(config.license()).isEqualTo(ProjectConfig.DEFAULT_LICENSE);
        assertThat(config.packageName()).isEqualTo(ProjectConfig.DEFAULT_PACKAGE_NAME);
        assertThat(config.labels()).isEmpty();
    }

    @Test
    void parseFromDirectoryParsesLabels() throws IOException {
        Files.writeString(tempDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), """
                schemaVersion: "1"
                name: my-app
                description: A test application
                labels:
                  - java
                  - spring-boot
                """);

        ProjectConfig config = ProjectConfigParser.parseFromDirectory(tempDir);

        assertThat(config.labels()).containsExactly("java", "spring-boot");
    }

    @Test
    void parseFromDirectoryIgnoresUnknownFields() throws IOException {
        Files.writeString(tempDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), """
                schemaVersion: "1"
                name: my-app
                description: A test application
                unknown: ignored
                """);

        assertThat(ProjectConfigParser.parseFromDirectory(tempDir)).isNotNull();
    }

    @Test
    void parseFromDirectoryThrowsWhenConfigFileNotFound() {
        assertThatThrownBy(() -> ProjectConfigParser.parseFromDirectory(tempDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("project.yml not found in directory");
    }

    @Test
    void parseFromDirectoryThrowsWhenYamlIsMalformed() throws IOException {
        Files.writeString(tempDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), """
                name: [unclosed bracket
                """);

        assertThatThrownBy(() -> ProjectConfigParser.parseFromDirectory(tempDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse project.yml");
    }

    @Test
    void parseFromDirectoryThrowsWhenNameIsMissing() throws IOException {
        Files.writeString(tempDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), """
                schemaVersion: "1"
                description: A test application
                """);

        assertThatThrownBy(() -> ProjectConfigParser.parseFromDirectory(tempDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse project.yml");
    }

    @Test
    void parseFromDirectoryThrowsWhenSchemaVersionIsMissing() throws IOException {
        Files.writeString(tempDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), """
                name: my-app
                description: A test application
                """);

        assertThatThrownBy(() -> ProjectConfigParser.parseFromDirectory(tempDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse project.yml");
    }

    @Test
    void parseFromDirectoryThrowsWhenDescriptionIsMissing() throws IOException {
        Files.writeString(tempDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), """
                schemaVersion: "1"
                name: my-app
                """);

        assertThatThrownBy(() -> ProjectConfigParser.parseFromDirectory(tempDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse project.yml");
    }

}
