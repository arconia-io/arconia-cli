package io.arconia.cli.project.oci;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectConfigWriter}.
 */
class ProjectConfigWriterTests {

    @TempDir
    Path tempDir;

    @Test
    void writeToDirectoryCreatesConfigFile() throws IOException {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-template")
                .description("A test template")
                .type("service")
                .license("Apache-2.0")
                .packageName("io.example.myapp")
                .labels(List.of("spring-boot", "http"))
                .build();

        ProjectConfigWriter.writeToDirectory(config, tempDir, false);

        Path configFile = tempDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME);
        assertThat(configFile).exists();

        // Verify it's valid YAML by parsing it back
        ProjectConfig parsed = ProjectConfigParser.parseFromDirectory(tempDir);
        assertThat(parsed.name()).isEqualTo("my-template");
        assertThat(parsed.description()).isEqualTo("A test template");
        assertThat(parsed.type()).isEqualTo("service");
        assertThat(parsed.license()).isEqualTo("Apache-2.0");
        assertThat(parsed.packageName()).isEqualTo("io.example.myapp");
        assertThat(parsed.labels()).containsExactly("spring-boot", "http");
        assertThat(parsed.schemaVersion()).isEqualTo(ProjectConfig.CURRENT_SCHEMA_VERSION);
    }

    @Test
    void writeToDirectoryUsesDefaults() throws IOException {
        ProjectConfig config = ProjectConfig.builder()
                .name("minimal")
                .description("A minimal template")
                .build();

        ProjectConfigWriter.writeToDirectory(config, tempDir, false);

        ProjectConfig parsed = ProjectConfigParser.parseFromDirectory(tempDir);
        assertThat(parsed.name()).isEqualTo("minimal");
        assertThat(parsed.description()).isEqualTo("A minimal template");
        assertThat(parsed.type()).isEqualTo(ProjectConfig.DEFAULT_TYPE);
        assertThat(parsed.license()).isEqualTo(ProjectConfig.DEFAULT_LICENSE);
        assertThat(parsed.packageName()).isEqualTo(ProjectConfig.DEFAULT_PACKAGE_NAME);
        assertThat(parsed.labels()).isEmpty();
    }

    @Test
    void writeToDirectoryThrowsWhenFileExistsAndForceIsFalse() throws IOException {
        Files.writeString(tempDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), "existing content");

        ProjectConfig config = ProjectConfig.builder()
                .name("my-template")
                .description("A test template")
                .build();

        assertThatThrownBy(() -> ProjectConfigWriter.writeToDirectory(config, tempDir, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists")
                .hasMessageContaining("--force");
    }

    @Test
    void writeToDirectoryOverwritesWhenForceIsTrue() throws IOException {
        Files.writeString(tempDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), "old content");

        ProjectConfig config = ProjectConfig.builder()
                .name("updated")
                .description("An updated template")
                .build();

        ProjectConfigWriter.writeToDirectory(config, tempDir, true);

        ProjectConfig parsed = ProjectConfigParser.parseFromDirectory(tempDir);
        assertThat(parsed.name()).isEqualTo("updated");
        assertThat(parsed.description()).isEqualTo("An updated template");
    }

    @Test
    void writeToDirectoryCreatesEmptyLabels() throws IOException {
        ProjectConfig config = ProjectConfig.builder()
                .name("no-labels")
                .description("Template without labels")
                .build();

        ProjectConfigWriter.writeToDirectory(config, tempDir, false);

        ProjectConfig parsed = ProjectConfigParser.parseFromDirectory(tempDir);
        assertThat(parsed.labels()).isEmpty();
    }

}
