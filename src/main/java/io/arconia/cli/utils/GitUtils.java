package io.arconia.cli.utils;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;

import io.arconia.cli.core.ProcessExecutor;

public final class GitUtils {

    private GitUtils() {}

    /**
     * Returns the current Git commit SHA (short form) for the given directory,
     * or {@code null} if the directory is not inside a Git repository.
     */
    @Nullable
    public static String getRevision(@Nullable Path workingDir) {
        return ProcessExecutor.executeAndGetOutput(
            new String[]{ "git", "rev-parse", "--short", "HEAD" },
            resolveDir(workingDir)
        );
    }

    /**
     * Returns the remote "origin" URL for the given directory,
     * or {@code null} if not available.
     */
    @Nullable
    public static String getRemoteUrl(@Nullable Path workingDir) {
        return ProcessExecutor.executeAndGetOutput(
            new String[]{ "git", "remote", "get-url", "origin" },
            resolveDir(workingDir)
        );
    }

    private static java.io.File resolveDir(@Nullable Path workingDir) {
        return workingDir != null ? workingDir.toFile() : Path.of(".").toAbsolutePath().toFile();
    }

}
