package io.arconia.cli.utils;

import java.io.File;

import java.io.IOException;

public final class ProcessUtils {

    public static void executeProcess(String[] arguments, File targetDirectory) {
        try {
            Process process = new ProcessBuilder()
                .command(arguments)
                .inheritIO()
                .directory(targetDirectory)
                .start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Process completed successfully.");
            } else {
                System.out.println("Process failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
