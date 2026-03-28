package io.arconia.cli.utils;

public final class SystemUtils {

    private SystemUtils() {}

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

}
