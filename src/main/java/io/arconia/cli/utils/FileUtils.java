package io.arconia.cli.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public final class FileUtils {

    public static Path getProjectDir() {
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    }

    public static File getExecutable(String executableName) {
        var pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            throw new RuntimeException("Cannot find the " + executableName + " command because the PATH environment variable is not set");
        }
    
        String[] pathDirs = pathEnv.split(Pattern.quote(File.pathSeparator));
        for (String dir : pathDirs) {
            File file = Paths.get(dir).resolve(executableName).toFile();
            if (file.exists() && file.canExecute()) {
                return file;
            }
        }

        throw new RuntimeException("Cannot find the " + executableName + " executable in the PATH");
    }

}
