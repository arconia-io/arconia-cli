package io.arconia.cli.utils;

/**
 * Utility methods for working with operating systems.
 */
public final class SystemUtils {

    private SystemUtils() {}

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

}
