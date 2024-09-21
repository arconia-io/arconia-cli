package io.arconia.cli.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public final class IoUtils {

    private IoUtils() {
        // Prevent instantiation
    }

    public static Path getProjectPath() {
        return getProjectPath(null);
    }

    public static Path getProjectPath(String path) {
        if (StringUtils.hasText(path)) {
            return Path.of(path);
        }

        return getWorkingDirectory();
    }

    public static Path getWorkingDirectory() {
        return Path.of(System.getProperty("user.dir")).toAbsolutePath();
    }

    @Nullable
    public static File getBuildToolWrapper(Path projectPath, String unixFileName, String windowsFileName) {
        Assert.notNull(projectPath, "projectPath cannot be null");
        Assert.hasText(unixFileName, "unixFileName cannot be null or empty");
        Assert.hasText(windowsFileName, "windowsFileName cannot be null or empty");

        Path currentPath = projectPath.toAbsolutePath().normalize();

        File wrapper;
        if (SystemUtils.isWindows()) {
            wrapper = currentPath.resolve(windowsFileName).toFile();
        } else {
            wrapper = currentPath.resolve(unixFileName).toFile();
        }

        if (wrapper.isFile()) {
            return wrapper;
        }

        if (currentPath.equals(projectPath.getRoot())) {
            return null;
        }

        return getBuildToolWrapper(currentPath.getParent(), unixFileName, windowsFileName);
    }

    @Nullable
    public static File getExecutable(String executableName) {
        Assert.hasText(executableName, "executableName cannot be null or empty");

        String pathEnv = System.getenv("PATH");
        if (!StringUtils.hasText(pathEnv)) {
            return null;
        }

        return Arrays.stream(pathEnv.split(Pattern.quote(File.pathSeparator)))
            .map(dir -> Path.of(dir).resolve(executableName).toFile())
            .filter(File::canExecute)
            .findFirst()
            .orElse(null);
    }

    public static Path copyFileToTemp(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        
        if (!resource.exists()) {
            throw new IOException("File not found: " + resourcePath);
        }

        Path tempFile = Files.createTempFile("temp-", "-" + resourcePath.substring(resourcePath.lastIndexOf('/') + 1));
        tempFile.toFile().deleteOnExit();
        
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }

}
