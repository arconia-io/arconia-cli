package io.arconia.cli.utils;

import java.io.File;
import java.io.IOException;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Utility methods for interacting with Git.
 */
public final class GitUtils {

    private GitUtils() {}

    /**
     * Returns the current Git commit SHA (short form) for the given directory,
     * or {@code null} if the directory is not inside a Git repository.
     */
    @Nullable
    public static String getRevision(@Nullable Path workingDir) {
        return executeGitCommand(resolveDir(workingDir), "rev-parse", "--short", "HEAD");
    }

    /**
     * Returns the remote "origin" URL for the given directory,
     * or {@code null} if not available.
     */
    @Nullable
    public static String getRemoteUrl(@Nullable Path workingDir) {
        return executeGitCommand(resolveDir(workingDir), "remote", "get-url", "origin");
    }

    @Nullable
    private static String executeGitCommand(File workingDir, String... args) {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);

        try {
            Process process = new ProcessBuilder()
                    .command(command)
                    .directory(workingDir)
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

    private static File resolveDir(@Nullable Path workingDir) {
        return workingDir != null ? workingDir.toFile() : IoUtils.getWorkingDirectory().toFile();
    }

}
