package io.arconia.cli.utils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class ProcessUtils {

    private ProcessUtils() {}

    /// Executes a command in the specified directory.
    ///
    /// @param arguments         The command and its arguments to be executed.
    /// @param targetDirectory   The directory in which to execute the command.
    ///
    public static void executeProcess(String[] command, File targetDirectory) {
        System.out.println("Running: %s".formatted(String.join(" ", command)));

        try {
            Process process = new ProcessBuilder()
                .command(command)
                .inheritIO()
                .directory(targetDirectory)
                .start();

            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                process.destroy();
                throw new ProcessExecutionException("Process timed out", command);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new ProcessExecutionException("Process failed with exit code %s".formatted(exitCode), command);
            }
        } catch (IOException ex) {
            throw new ProcessExecutionException("IO error occurred while executing process", command, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            throw new ProcessExecutionException("Process was interrupted", command, ex);
        }
    }

}
