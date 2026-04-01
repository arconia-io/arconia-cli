package io.arconia.cli.utils;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;

import io.arconia.cli.core.ProcessExecutor;

public final class GitUtils {

    private GitUtils() {}

    /**
     * Returns the current Git commit SHA (short form) for the given directory,
     * or {@code null} if the directory is not inside a Git repository.
     *
     * @param workingDir the directory to inspect
     * @return the short commit SHA, or {@code null}
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
     *
     * @param workingDir the directory to inspect, or {@code null} for the current directory
     * @return the origin URL, or {@code null}
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
