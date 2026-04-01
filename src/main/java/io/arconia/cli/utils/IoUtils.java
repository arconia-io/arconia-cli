package io.arconia.cli.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public final class IoUtils {

    private IoUtils() {}

    public static Path getProjectPath() {
        return getProjectPath(null);
    }

    public static Path getProjectPath(@Nullable String path) {
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

    /**
     * Safely deletes an installed skill directory.
     * <p>
     * This method enforces two safety invariants before deleting:
     * <ol>
     *   <li>The directory must be located under the expected skills path
     *       (e.g., {@code .agents/skills/}) relative to the project root.</li>
     *   <li>The directory must contain a {@code SKILL.md} file, confirming
     *       it is a valid skill directory.</li>
     * </ol>
     * If either check fails, the method throws {@link IllegalArgumentException}
     * rather than proceeding with a potentially destructive delete.
     *
     * @param skillDir the skill directory to delete
     * @param projectRoot the project root directory
     * @param skillsBasePath the expected skills base path relative to the project root
     *                       (e.g., {@code ".agents/skills"})
     * @throws IOException if the directory cannot be deleted
     * @throws IllegalArgumentException if the path fails safety validation
     */
    public static void deleteSkillDirectory(Path skillDir, Path projectRoot, String skillsBasePath) throws IOException {
        Assert.notNull(skillDir, "skillDir must not be null");
        Assert.notNull(projectRoot, "projectRoot must not be null");
        Assert.hasText(skillsBasePath, "skillsBasePath must not be empty");

        Path normalizedSkillDir = skillDir.toAbsolutePath().normalize();
        Path normalizedSkillsBase = projectRoot.toAbsolutePath().normalize().resolve(skillsBasePath);

        // Safety check 1: must be inside the skills base directory
        if (!normalizedSkillDir.startsWith(normalizedSkillsBase)) {
            throw new IllegalArgumentException(
                "Refusing to delete '%s': not inside the skills directory '%s'".formatted(
                    normalizedSkillDir, normalizedSkillsBase));
        }

        // Safety check 2: must be a direct child of the skills base (not deeper nesting)
        if (!normalizedSkillDir.getParent().equals(normalizedSkillsBase)) {
            throw new IllegalArgumentException(
                "Refusing to delete '%s': must be a direct child of '%s'".formatted(
                    normalizedSkillDir, normalizedSkillsBase));
        }

        // Safety check 3: must contain SKILL.md
        if (!Files.exists(normalizedSkillDir.resolve("SKILL.md"))) {
            throw new IllegalArgumentException(
                "Refusing to delete '%s': directory does not contain SKILL.md".formatted(
                    normalizedSkillDir));
        }

        // Walk depth-first and delete all entries
        try (Stream<Path> walk = Files.walk(normalizedSkillDir)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    }
                    catch (IOException e) {
                        // Best-effort: log but continue
                    }
                });
        }
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
