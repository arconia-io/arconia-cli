package io.arconia.cli.core;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import io.arconia.cli.commands.options.OutputOptions;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CliExceptionHandler}.
 */
class CliExceptionHandlerTests {

    private final CliExceptionHandler handler = new CliExceptionHandler();

    private CommandLine createCommandLine() {
        var spec = CommandSpec.create()
            .name("test-cmd")
            .addMixin("outputOptions", CommandSpec.forAnnotatedObject(new OutputOptions()));
        return new CommandLine(spec);
    }

    @Test
    void shouldPrintExceptionMessageAndReturnExitCode() {
        var output = new StringWriter();
        var cmd = createCommandLine();
        cmd.setErr(new PrintWriter(output));

        int exitCode = handler.handleExecutionException(
            new CliException("Something went wrong"), cmd, cmd.parseArgs());

        assertThat(exitCode).isEqualTo(cmd.getCommandSpec().exitCodeOnExecutionException());
        assertThat(output.toString()).contains("Something went wrong");
    }

    @Test
    void shouldUseFallbackMessageWhenExceptionMessageIsNull() {
        var output = new StringWriter();
        var cmd = createCommandLine();
        cmd.setErr(new PrintWriter(output));

        int exitCode = handler.handleExecutionException(
            new CliException(null), cmd, cmd.parseArgs());

        assertThat(exitCode).isEqualTo(cmd.getCommandSpec().exitCodeOnExecutionException());
        assertThat(output.toString()).contains("An unexpected error occurred");
    }

    @Test
    void shouldPrintStackTraceWhenVerboseOptionIsMatched() {
        var output = new StringWriter();
        var cmd = createCommandLine();
        cmd.setErr(new PrintWriter(output));

        int exitCode = handler.handleExecutionException(
            new CliException("Verbose error"), cmd, cmd.parseArgs("--verbose"));

        assertThat(exitCode).isEqualTo(cmd.getCommandSpec().exitCodeOnExecutionException());
        assertThat(output.toString()).contains("Verbose error");
        assertThat(output.toString()).contains("CliException");
        System.out.println(output.toString());
    }

    @Test
    void shouldNotPrintStackTraceWhenVerboseOptionIsNotMatched() {
        var output = new StringWriter();
        var cmd = createCommandLine();
        cmd.setErr(new PrintWriter(output));

        handler.handleExecutionException(
            new CliException("Simple error"), cmd, cmd.parseArgs());

        assertThat(output.toString()).contains("Simple error");
        assertThat(output.toString()).doesNotContain("CliException");
    }

}
