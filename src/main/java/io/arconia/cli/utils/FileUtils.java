package io.arconia.cli.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;

public final class FileUtils {

    /// Get the absolute path of the current project directory.
    ///
    /// @return the absolute path of the current working directory
    ///
    public static Path getProjectDir() {
        return Path.of(System.getProperty("user.dir")).toAbsolutePath();
    }

    /// Find the given executable in the system's PATH.
    ///
    /// @param  executableName  the name of the executable to find
    /// @return                 the found executable file
    ///
    public static File getExecutable(String executableName) {
        var pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            throw new RuntimeException("Cannot find the " + executableName + " command because the PATH environment variable is not set");
        }
    
        String[] pathDirs = pathEnv.split(Pattern.quote(File.pathSeparator));
        for (String dir : pathDirs) {
            File file = Path.of(dir).resolve(executableName).toFile();
            if (file.exists() && file.canExecute()) {
                return file;
            }
        }

        throw new RuntimeException("Cannot find the " + executableName + " executable in the PATH");
    }

    /// Copy a file from the classpath to a temporary file.
    ///
    /// @param  resourcePath    the path of the resource in the classpath
    /// @return                 the created temporary file
    /// @throws IOException     if the file is not found or if there's an error during file operations
    ///
    public static Path copyFileToTemp(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        
        if (!resource.exists()) {
            throw new IOException("File not found: " + resourcePath);
        }

        Path tempFile = Files.createTempFile("temp-", "-" + getFileNameFromPath(resourcePath));
        tempFile.toFile().deleteOnExit();
        
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }

    private static String getFileNameFromPath(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

}
