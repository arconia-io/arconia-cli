package io.arconia.cli.core;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

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

    // --- executeAndGetOutput ---

    @Test
    void executeAndGetOutput() {
        String result = ProcessExecutor.executeAndGetOutput(new String[]{"echo", "hello world"}, tempDir);
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    void executeAndGetOutputReturnsOnlyFirstLine() {
        String result = ProcessExecutor.executeAndGetOutput(new String[]{"printf", "line1\nline2\nline3"}, tempDir);
        assertThat(result).isEqualTo("line1");
    }

    @Test
    void executeAndGetOutputReturnsNullForFailingCommand() {
        String result = ProcessExecutor.executeAndGetOutput(new String[]{"false"}, tempDir);
        assertThat(result).isNull();
    }

    @Test
    void executeAndGetOutputReturnsNullForNonExistentCommand() {
        String result = ProcessExecutor.executeAndGetOutput(new String[]{"nonexistent-command-xyz"}, tempDir);
        assertThat(result).isNull();
    }

    @Test
    void executeAndGetOutputRejectsNullCommand() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProcessExecutor.executeAndGetOutput(null, tempDir))
            .withMessage("command cannot be null or empty");
    }

    @Test
    void executeAndGetOutputRejectsNullTargetDirectory() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProcessExecutor.executeAndGetOutput(new String[]{"echo", "test"}, null))
            .withMessage("targetDirectory cannot be null");
    }

}
