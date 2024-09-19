package io.arconia.cli.utils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class ProcessUtils {

    private ProcessUtils() {}

    /// Executes a process with the given arguments in the specified directory.
    ///
    /// @param arguments         The command and its arguments to be executed.
    /// @param targetDirectory   The directory in which to execute the command.
    ///
    public static void executeProcess(String[] arguments, File targetDirectory) {
        System.out.println("Running: %s".formatted(String.join(" ", arguments)));

        try {
            Process process = new ProcessBuilder()
                .command(arguments)
                .inheritIO()
                .directory(targetDirectory)
                .start();

            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                System.out.println("Process timed out: " + String.join(" ", arguments));
                process.destroy();
                return;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                System.out.println("Process failed with exit code %s: %s".formatted(exitCode, String.join(" ", arguments)));
            }
        } catch (IOException e) {
            throw new RuntimeException("IO error occurred while executing process: %s".formatted(String.join(" ", arguments)), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            throw new RuntimeException("Process was interrupted: %s".formatted(String.join(" ", arguments)), e);
        }
    }

}
