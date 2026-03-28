package io.arconia.cli.core;

import java.io.File;
import java.io.IOException;

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

}
