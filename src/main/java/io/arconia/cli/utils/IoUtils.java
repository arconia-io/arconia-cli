package io.arconia.cli.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility methods for file system operations.
 */
public final class IoUtils {

    public static final String TEMP_DIR_PREFIX = ".arconia-tmp-";

    private IoUtils() {}

    /**
     * Returns the path to the project directory.
     */
    public static Path getProjectPath() {
        return getProjectPath(null);
    }

    /**
     * Returns the path to the project directory for the given path.
     */
    public static Path getProjectPath(@Nullable String path) {
        if (StringUtils.hasText(path)) {
            return Path.of(path).toAbsolutePath();
        }

        return getWorkingDirectory();
    }

    /**
     * Returns the path to the current working directory.
     */
    public static Path getWorkingDirectory() {
        return Path.of(System.getProperty("user.dir")).toAbsolutePath();
    }

    /**
     * Discovers all subdirectories under the given parent directory that contain a file
     * with the given name.
     */
    public static List<Path> discoverSubDirectoriesWithFile(Path parentPath, String fileName) throws IOException {
        Assert.notNull(parentPath, "parentPath cannot be null");
        Assert.hasText(fileName, "fileName cannot be null or empty");

        List<Path> projectDirs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && Files.exists(entry.resolve(fileName))) {
                    projectDirs.add(entry);
                }
            }
        }
        projectDirs.sort(Path::compareTo);
        return projectDirs;
    }

    /**
     * Recursively deletes empty directories starting from the given directory.
     * <p>
     * Traverses depth-first so that after a nested empty directory is removed,
     * its now-empty parent is also eligible for deletion.
     * Only directories that contain no remaining entries are deleted; directories
     * that still contain files or non-empty subdirectories are left untouched.
     */
    public static void deleteEmptyDirectoriesRecursively(Path directory) throws IOException {
        Assert.notNull(directory, "directory cannot be null");

        Path normalizedDir = directory.toAbsolutePath().normalize();

        if (!Files.isDirectory(normalizedDir)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(normalizedDir)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        try (Stream<Path> contents = Files.list(dir)) {
                            if (contents.findAny().isEmpty()) {
                                Files.deleteIfExists(dir);
                            }
                        }
                        catch (IOException ignored) {
                            // Best-effort: continue on failure.
                        }
                    });
        }
    }

    /**
     * Copies a file from the classpath to a temporary file.
     */
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
     * Creates a temporary file with owner-only permissions.
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

    //------

    /**
     * Finds the first {@code *.tar.gz} file in the given directory.
     */
    public static Path findTarGzFile(Path directory) throws IOException {
        Assert.notNull(directory, "directory cannot be null");
        try (var stream = Files.newDirectoryStream(directory, "*.tar.gz")) {
            for (Path entry : stream) {
                return entry;
            }
        }
        throw new IOException("No tar.gz file found in directory: " + directory);
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
     * Creates a per-skill symbolic link at {@code link} pointing to the primary skill
     * directory at {@code target}.
     * <p>
     * For example, given:
     * <ul>
     *   <li>{@code link}   = {@code /project/.claude/skills/my-skill}</li>
     *   <li>{@code target} = {@code /project/.agents/skills/my-skill}</li>
     * </ul>
     * The resulting symlink uses a relative path: {@code ../../.agents/skills/my-skill}.
     * <p>
     * Parent directories of the link are created as needed. If a symlink or directory
     * already exists at the link location, it is removed first.
     * <p>
     * On systems where symbolic links are not supported or not permitted (e.g. Windows
     * without Developer Mode), the method falls back to a recursive directory copy.
     *
     * @param link the path where the symlink should be created (vendor skill directory)
     * @param target the path the symlink should point to (primary skill directory)
     * @throws IOException if neither the symlink nor the fallback copy can be created
     */
    public static void createSkillSymlink(Path link, Path target) throws IOException {
        Assert.notNull(link, "link must not be null");
        Assert.notNull(target, "target must not be null");

        Files.createDirectories(link.getParent());

        // Remove existing entry (symlink or directory) at the link location
        if (Files.isSymbolicLink(link)) {
            Files.delete(link);
        }
        else if (Files.isDirectory(link)) {
            deleteDirectoryRecursive(link);
        }

        // Try symlink first; fall back to copy on platforms that don't support it
        try {
            Path relativePath = link.getParent().relativize(target);
            Files.createSymbolicLink(link, relativePath);
        }
        catch (UnsupportedOperationException | SecurityException | IOException e) {
            // Symlinks not available (e.g. Windows without Developer Mode) – copy instead
            copyDirectory(target, link);
        }
    }

    /**
     * Recursively copies an entire directory tree from source to target.
     * <p>
     * Used as a fallback when symbolic links are not available.
     */
    private static void copyDirectory(Path source, Path target) throws IOException {
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
                catch (IOException ex) {
                    throw new RuntimeException("Failed to copy '%s': %s".formatted(sourcePath, ex.getMessage()), ex);
                }
            });
        }
    }

    /**
     * Deletes a symlink, or a regular file/directory.
     * <p>
     * If the path is a symbolic link, only the link itself is removed (not its target).
     * If the path is a directory, it is deleted recursively.
     *
     * @param path the path to delete
     * @throws IOException if deletion fails
     */
    public static void deleteSymlinkOrDirectory(Path path) throws IOException {
        Assert.notNull(path, "path must not be null");

        if (!Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        if (Files.isSymbolicLink(path)) {
            Files.delete(path);
        }
        else if (Files.isDirectory(path)) {
            deleteDirectoryRecursive(path);
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    private static void deleteDirectoryRecursive(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
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

    /**
     * Creates a temporary directory with owner-only permissions under the given parent directory.
     */
    public static Path createTempDirectory(Path parentDirectory) throws IOException {
        Assert.notNull(parentDirectory, "parentDirectory cannot be null");

        String prefix = TEMP_DIR_PREFIX;
        Files.createDirectories(parentDirectory);
        if (isPosixFileSystem()) {
            FileAttribute<Set<PosixFilePermission>> attr = ownerOnlyDirPosixAttribute();
            return Files.createTempDirectory(parentDirectory, prefix, attr);
        }
        return Files.createTempDirectory(parentDirectory, prefix);
    }

    /**
     * Safely deletes a temporary directory and all its contents on a best-effort basis.
     * <p>
     * Enforces that the directory name starts with the expected temp prefix to prevent
     * accidental deletion of unrelated directories. Individual entry deletions are
     * best-effort: failures are ignored so that a partially cleaned-up temp directory
     * does not surface as an error to the user. Given the explicit prefix naming convention,
     * a partially cleaned-up directory can be recognized by the user and manually removed.
     */
    public static void deleteTempDirectory(Path tempDir) throws IOException {
        Assert.notNull(tempDir, "tempDir must not be null");

        Path normalizedTempDir = tempDir.toAbsolutePath().normalize();

        String dirName = normalizedTempDir.getFileName() != null ? normalizedTempDir.getFileName().toString() : "";
        if (!dirName.startsWith(TEMP_DIR_PREFIX)) {
            throw new IllegalArgumentException(
                "Refusing to delete '%s': directory name does not start with '%s'"
                    .formatted(normalizedTempDir, TEMP_DIR_PREFIX));
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
                    catch (IOException ignored) {
                        // Best-effort: a leftover temp file is cosmetic, not a failure.
                    }
                });
        }
    }

    private static FileAttribute<Set<PosixFilePermission>> ownerOnlyDirPosixAttribute() {
        return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
    }

    private static FileAttribute<Set<PosixFilePermission>> ownerOnlyFilePosixAttribute() {
        return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
    }

}
