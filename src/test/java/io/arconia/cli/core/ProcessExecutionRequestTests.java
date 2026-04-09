package io.arconia.cli.core;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import io.arconia.cli.commands.options.OutputOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProcessExecutionRequest}.
 */
class ProcessExecutionRequestTests {

    @TempDir
    File tempDir;

    private OutputOptions createOutputOptions() {
        var outputOptions = new OutputOptions();
        var spec = CommandSpec.create()
            .name("test-cmd")
            .addMixin("outputOptions", CommandSpec.forAnnotatedObject(outputOptions));
        var cmd = new CommandLine(spec);
        cmd.setOut(new PrintWriter(new StringWriter()));
        cmd.setErr(new PrintWriter(new StringWriter()));
        cmd.parseArgs();
        return outputOptions;
    }

    @Test
    void buildsSuccessfullyWithRequiredFields() {
        var request = ProcessExecutionRequest.builder()
                .command(new String[]{"echo", "hello"})
                .targetDirectory(tempDir)
                .outputOptions(createOutputOptions())
                .build();

        assertThat(request.command()).containsExactly("echo", "hello");
        assertThat(request.targetDirectory()).isEqualTo(tempDir);
        assertThat(request.environmentVariables()).isEmpty();
        assertThat(request.outputOptions()).isNotNull();
    }

    @Test
    void buildsSuccessfullyWithAllFields() {
        var request = ProcessExecutionRequest.builder()
                .command(new String[]{"echo", "hello"})
                .targetDirectory(tempDir)
                .environmentVariables(Map.of("MY_VAR", "my_value"))
                .outputOptions(createOutputOptions())
                .build();

        assertThat(request.command()).containsExactly("echo", "hello");
        assertThat(request.environmentVariables()).containsEntry("MY_VAR", "my_value");
    }

    @Test
    void whenCommandIsNullThenThrow() {
        assertThatThrownBy(() -> ProcessExecutionRequest.builder()
                .targetDirectory(tempDir)
                .outputOptions(createOutputOptions())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("command cannot be null or empty");
    }

    @Test
    void whenCommandIsEmptyThenThrow() {
        assertThatThrownBy(() -> ProcessExecutionRequest.builder()
                .command(new String[]{})
                .targetDirectory(tempDir)
                .outputOptions(createOutputOptions())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("command cannot be null or empty");
    }

    @Test
    void whenTargetDirectoryIsNullThenThrow() {
        assertThatThrownBy(() -> ProcessExecutionRequest.builder()
                .command(new String[]{"true"})
                .outputOptions(createOutputOptions())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetDirectory cannot be null");
    }

    @Test
    void whenTargetDirectoryDoesNotExistThenThrow() {
        var nonExistentDir = new File(tempDir, "does-not-exist");
        assertThatThrownBy(() -> ProcessExecutionRequest.builder()
                .command(new String[]{"true"})
                .targetDirectory(nonExistentDir)
                .outputOptions(createOutputOptions())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetDirectory must be an existing directory");
    }

    @Test
    void whenEnvironmentVariablesIsNullThenThrow() {
        assertThatThrownBy(() -> ProcessExecutionRequest.builder()
                .command(new String[]{"true"})
                .targetDirectory(tempDir)
                .environmentVariables(null)
                .outputOptions(createOutputOptions())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("environmentVariables cannot be null");
    }

    @Test
    void whenOutputOptionsIsNullThenThrow() {
        assertThatThrownBy(() -> ProcessExecutionRequest.builder()
                .command(new String[]{"true"})
                .targetDirectory(tempDir)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outputOptions cannot be null");
    }

    @Test
    void environmentVariablesDefaultsToEmpty() {
        var request = ProcessExecutionRequest.builder()
                .command(new String[]{"true"})
                .targetDirectory(tempDir)
                .outputOptions(createOutputOptions())
                .build();

        assertThat(request.environmentVariables()).isEmpty();
    }

}
