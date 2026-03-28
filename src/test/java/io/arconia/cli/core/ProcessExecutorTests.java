package io.arconia.cli.core;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.arconia.cli.commands.options.OutputOptions;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

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

    @Test
    void executeSuccessfulCommand() {
        int exitCode = ProcessExecutor.execute(createOutputOptions(), new String[]{"true"}, tempDir);
        assertThat(exitCode).isZero();
    }

    @Test
    void executeFailingCommand() {
        int exitCode = ProcessExecutor.execute(createOutputOptions(), new String[]{"false"}, tempDir);
        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void executeNonExistentCommandThrowsArconiaCliException() {
        assertThatThrownBy(() -> ProcessExecutor.execute(
                createOutputOptions(), new String[]{"nonexistent-command-xyz"}, tempDir))
            .isInstanceOf(CliException.class);
    }

    @Test
    void executeRejectsNullOutputOptions() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProcessExecutor.execute(null, new String[]{"true"}, tempDir))
            .withMessage("outputOptions cannot be null");
    }

    @Test
    void executeRejectsNullCommand() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProcessExecutor.execute(createOutputOptions(), null, tempDir))
            .withMessage("command cannot be null or empty");
    }

    @Test
    void executeRejectsEmptyCommand() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProcessExecutor.execute(createOutputOptions(), new String[]{}, tempDir))
            .withMessage("command cannot be null or empty");
    }

    @Test
    void executeRejectsNullTargetDirectory() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProcessExecutor.execute(createOutputOptions(), new String[]{"true"}, null))
            .withMessage("targetDirectory cannot be null");
    }

    @Test
    void executeRejectsNonExistentTargetDirectory() {
        var nonExistentDir = new File(tempDir, "does-not-exist");
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProcessExecutor.execute(createOutputOptions(), new String[]{"true"}, nonExistentDir))
            .withMessage("targetDirectory must be an existing directory");
    }

}
