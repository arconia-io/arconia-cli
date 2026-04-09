package io.arconia.cli.core;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import io.arconia.cli.commands.options.OutputOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProcessExecutor}.
 */
class ProcessExecutorTests {

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

    private ProcessExecutionRequest.Builder requestBuilder() {
        return ProcessExecutionRequest.builder()
            .targetDirectory(tempDir)
            .outputOptions(createOutputOptions());
    }

    @Test
    void executeSuccessfulCommand() {
        int exitCode = ProcessExecutor.execute(requestBuilder()
            .command(List.of("true"))
            .build());
        assertThat(exitCode).isZero();
    }

    @Test
    void executeFailingCommand() {
        int exitCode = ProcessExecutor.execute(requestBuilder()
            .command(List.of("false"))
            .build());
        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void executeNonExistentCommandThrowsCliException() {
        assertThatThrownBy(() -> ProcessExecutor.execute(requestBuilder()
                .command(List.of("nonexistent-command-xyz"))
                .build()))
            .isInstanceOf(CliException.class);
    }

    @Test
    void executeWithEnvironmentVariables() {
        int exitCode = ProcessExecutor.execute(requestBuilder()
            .command(List.of("true"))
            .environmentVariables(Map.of("MY_VAR", "my_value"))
            .build());
        assertThat(exitCode).isZero();
    }

    @Test
    void executeRejectsNullRequest() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProcessExecutor.execute(null))
            .withMessage("processExecutionRequest cannot be null");
    }

}
