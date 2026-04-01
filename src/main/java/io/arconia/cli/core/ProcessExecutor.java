package io.arconia.cli.core;

import java.io.File;
import java.io.IOException;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.commands.options.OutputOptions;

/**
 * Executes external processes on behalf of the CLI.
 */
public final class ProcessExecutor {

    private ProcessExecutor() {}

    /**
     * Executes the given command in the specified directory, inheriting the parent process I/O streams.
     */
    public static int execute(OutputOptions outputOptions, String[] command, File targetDirectory) {
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        Assert.notEmpty(command, "command cannot be null or empty");
        Assert.notNull(targetDirectory, "targetDirectory cannot be null");
        Assert.isTrue(targetDirectory.isDirectory(), "targetDirectory must be an existing directory");

        outputOptions.verbose(
            "Executing: %s".formatted(String.join(" ", command)),
            "Project: %s".formatted(targetDirectory));

        try {
            return new ProcessBuilder()
                .command(command)
                .inheritIO()
                .directory(targetDirectory)
                .start()
                .waitFor();
        } catch (IOException ex) {
            throw new CliException(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CliException("Process was interrupted", ex);
        }
    }

    /**
     * Executes the given command in the specified directory and captures its standard output.
     * Returns the trimmed output, or {@code null} if the command fails (non-zero exit code
     * or empty output).
     */
    @Nullable
    public static String executeAndGetOutput(String[] command, File targetDirectory) {
        Assert.notEmpty(command, "command cannot be null or empty");
        Assert.notNull(targetDirectory, "targetDirectory cannot be null");

        try {
            Process process = new ProcessBuilder()
                .command(command)
                .directory(targetDirectory)
                .redirectErrorStream(true)
                .start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0 || output.isEmpty()) {
                return null;
            }
            int newline = output.indexOf('\n');
            return newline >= 0 ? output.substring(0, newline).trim() : output;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

}
