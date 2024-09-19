package io.arconia.cli.utils;

public final class SystemUtils {

    /// Checks if the current operating system is Windows.
    ///
    /// @return true if the operating system is Windows, false otherwise
    ///
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

}
