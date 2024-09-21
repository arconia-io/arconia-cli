package io.arconia.cli.core;

import java.io.File;
import java.io.IOException;

import org.springframework.util.Assert;

public final class ProcessExecutor {

    private ProcessExecutor() {
        // Prevent instantiation
    }

    public static int execute(ArconiaCliTerminal terminal, String[] command, File targetDirectory) {
        Assert.notNull(terminal, "terminal cannot be null");
        Assert.notEmpty(command, "command cannot be null or empty");
        Assert.notNull(targetDirectory, "targetDirectory cannot be null");
        
        terminal.verbose(
            "🚀 Executing: %s".formatted(String.join(" ", command)),
            "📁 Project: %s".formatted(targetDirectory));

        try {
            Process process = new ProcessBuilder()
                .command(command)
                .inheritIO()
                .directory(targetDirectory)
                .start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                terminal.verbose("\n🍃 Mission accomplished!");
            } else {
                terminal.verbose("\n🍂 Ouch! Something went wrong!");
            }
            return exitCode;
        } catch (IOException ex) {
            throw new ArconiaCliException(terminal, ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            throw new ArconiaCliException(terminal, ex.getMessage(), ex);
        }
    }

}
