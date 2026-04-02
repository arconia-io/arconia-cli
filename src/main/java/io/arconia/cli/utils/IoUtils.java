package io.arconia.cli.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
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
     *                       (e.g., {@code ".agents/skills"} or {@code ".claude/skills"})
     * @throws IOException if the directory cannot be deleted
     * @throws IllegalArgumentException if the path fails safety validation
     */
    public static void deleteSkillDirectory(Path skillDir, Path projectRoot, String skillsBasePath) throws IOException {
        Assert.notNull(skillDir, "skillDir must not be null");
        Assert.notNull(projectRoot, "projectRoot must not be null");
        Assert.hasText(skillsBasePath, "skillsBasePath must not be empty");

        Path normalizedSkillDir = skillDir.toRealPath();
        Path normalizedSkillsBase = projectRoot.toRealPath().resolve(skillsBasePath);

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

    /**
     * Recursively copies an entire directory tree from source to target.
     * <p>
     * Creates the target directory and all parent directories as needed.
     * If the target already exists, its contents are overwritten.
     *
     * @param source the source directory to copy from
     * @param target the target directory to copy to
     * @throws IOException if the directory cannot be copied
     */
    public static void copyDirectory(Path source, Path target) throws IOException {
        Assert.notNull(source, "source must not be null");
        Assert.notNull(target, "target must not be null");

        try (Stream<Path> walk = Files.walk(source)) {
            walk.forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    }
                    else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                catch (IOException e) {
                    throw new RuntimeException("Failed to copy '%s': %s".formatted(sourcePath, e.getMessage()), e);
                }
            });
        }
    }

    /**
     * Derives the skills base path from a full skill installation path.
     * <p>
     * For example, given {@code .claude/skills/pull-request}, returns {@code .claude/skills}.
     *
     * @param skillPath the full relative skill path (e.g. {@code .claude/skills/pull-request})
     * @return the base path (e.g. {@code .claude/skills})
     */
    public static String deriveSkillsBasePath(String skillPath) {
        Assert.hasText(skillPath, "skillPath must not be empty");

        Path path = Path.of(skillPath);
        Path parent = path.getParent();
        if (parent == null) {
            throw new IllegalArgumentException(
                "Cannot derive skills base path from '%s': no parent directory".formatted(skillPath));
        }
        return parent.toString();
    }

    public static Path copyFileToTemp(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            throw new IOException("File not found: " + resourcePath);
        }

        Path tempFile = createTempFile("temp-", "-" + resourcePath.substring(resourcePath.lastIndexOf('/') + 1));
        tempFile.toFile().deleteOnExit();

        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }

    /**
     * Creates a temporary directory with owner-only permissions under the given parent
     * directory.
     *
     * @param parentDir the parent directory in which to create the temp directory
     * @param prefix the prefix for the temporary directory name
     * @return the path to the created temporary directory
     * @throws IOException if the directory cannot be created
     */
    public static Path createTempDirectory(Path parentDir, String prefix) throws IOException {
        Assert.notNull(parentDir, "parentDir must not be null");
        Assert.hasText(prefix, "prefix must not be empty");
        Files.createDirectories(parentDir);
        if (isPosixFileSystem()) {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
                .asFileAttribute(PosixFilePermissions.fromString("rwx------"));
            return Files.createTempDirectory(parentDir, prefix, attr);
        }
        return Files.createTempDirectory(parentDir, prefix);
    }

    /**
     * Safely deletes a temporary directory and all its contents.
     * <p>
     * Enforces that the directory is a direct child of the expected parent to prevent
     * accidental deletion of unrelated directories.
     *
     * @param tempDir the temporary directory to delete
     * @param expectedParent the parent directory that must contain the temp directory
     * @throws IOException if the directory cannot be deleted
     * @throws IllegalArgumentException if the temp directory is not a direct child of the
     * expected parent
     */
    public static void deleteTempDirectory(Path tempDir, Path expectedParent) throws IOException {
        Assert.notNull(tempDir, "tempDir must not be null");
        Assert.notNull(expectedParent, "expectedParent must not be null");

        Path normalizedTempDir = tempDir.toAbsolutePath().normalize();
        Path normalizedParent = expectedParent.toAbsolutePath().normalize();

        // Safety check: must be a direct child of the expected parent
        if (normalizedTempDir.getParent() == null
                || !normalizedTempDir.getParent().equals(normalizedParent)) {
            throw new IllegalArgumentException(
                "Refusing to delete '%s': not a direct child of '%s'".formatted(
                    normalizedTempDir, normalizedParent));
        }

        if (!Files.exists(normalizedTempDir)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(normalizedTempDir)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    }
                    catch (IOException e) {
                        // Best-effort: continue on failure
                    }
                });
        }
    }

    /**
     * Creates a temporary file with owner-only permissions.
     *
     * @param prefix the prefix for the temporary file name
     * @param suffix the suffix for the temporary file name
     * @return the path to the created temporary file
     * @throws IOException if the file cannot be created
     */
    public static Path createTempFile(String prefix, String suffix) throws IOException {
        if (isPosixFileSystem()) {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
                .asFileAttribute(PosixFilePermissions.fromString("rw-------"));
            return Files.createTempFile(prefix, suffix, attr);
        }
        return Files.createTempFile(prefix, suffix);
    }

    private static boolean isPosixFileSystem() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

}
