package io.arconia.cli.core;

import java.io.IOException;

import org.springframework.util.Assert;

/**
 * Executes external processes on behalf of the CLI.
 */
public final class ProcessExecutor {

    private ProcessExecutor() {}

    /**
     * Executes the given command in the specified directory, inheriting the parent process I/O streams.
     */
    public static int execute(ProcessExecutionRequest processExecutionRequest) {
        Assert.notNull(processExecutionRequest, "processExecutionRequest cannot be null");

        processExecutionRequest.outputOptions().verbose(
            "Executing: %s".formatted(String.join(" ", processExecutionRequest.command())),
            "Path: %s".formatted(processExecutionRequest.targetDirectory()));

        try {
            var processBuilder = new ProcessBuilder()
                .command(processExecutionRequest.command())
                .inheritIO()
                .directory(processExecutionRequest.targetDirectory());
            if (!processExecutionRequest.environmentVariables().isEmpty()) {
                processBuilder.environment().putAll(processExecutionRequest.environmentVariables());
            }
            return processBuilder
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
