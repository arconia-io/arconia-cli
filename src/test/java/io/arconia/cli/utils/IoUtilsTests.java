package io.arconia.cli.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link IoUtils}.
 */
class IoUtilsTests {

    @TempDir
    Path tempDir;

    // --- getProjectPath ---

    @Test
    void getProjectPathWithNoArgReturnsWorkingDirectory() {
        assertThat(IoUtils.getProjectPath()).isEqualTo(IoUtils.getWorkingDirectory());
    }

    @Test
    void getProjectPathWithNullReturnsWorkingDirectory() {
        assertThat(IoUtils.getProjectPath(null)).isEqualTo(IoUtils.getWorkingDirectory());
    }

    @Test
    void getProjectPathWithEmptyStringReturnsWorkingDirectory() {
        assertThat(IoUtils.getProjectPath("")).isEqualTo(IoUtils.getWorkingDirectory());
    }

    @Test
    void getProjectPathWithExplicitPathReturnsGivenPath() {
        assertThat(IoUtils.getProjectPath(tempDir.toString())).isEqualTo(tempDir);
    }

    // --- getWorkingDirectory ---

    @Test
    void getWorkingDirectoryReturnsAbsolutePath() {
        Path result = IoUtils.getWorkingDirectory();
        assertThat(result).isAbsolute();
    }

    @Test
    void getWorkingDirectoryMatchesUserDirSystemProperty() {
        Path result = IoUtils.getWorkingDirectory();
        assertThat(result.toString()).isEqualTo(Path.of(System.getProperty("user.dir")).toAbsolutePath().toString());
    }

    // --- getBuildToolWrapper ---

    @Test
    void getBuildToolWrapperThrowsWhenProjectPathIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.getBuildToolWrapper(null, "gradlew", "gradlew.bat"))
                .withMessage("projectPath cannot be null");
    }

    @Test
    void getBuildToolWrapperThrowsWhenUnixFileNameIsEmpty() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.getBuildToolWrapper(tempDir, "", "gradlew.bat"));
    }

    @Test
    void getBuildToolWrapperThrowsWhenWindowsFileNameIsEmpty() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.getBuildToolWrapper(tempDir, "gradlew", ""));
    }

    @Test
    void getBuildToolWrapperReturnsWrapperWhenFoundInProjectDir() throws IOException {
        File wrapper = Files.createFile(tempDir.resolve("gradlew")).toFile();

        File result = IoUtils.getBuildToolWrapper(tempDir, "gradlew", "gradlew.bat");

        assertThat(result).isNotNull().isEqualTo(wrapper);
    }

    @Test
    void getBuildToolWrapperReturnsNullWhenNotFound() {
        File result = IoUtils.getBuildToolWrapper(tempDir, "gradlew", "gradlew.bat");

        assertThat(result).isNull();
    }

    @Test
    void getBuildToolWrapperSearchesParentDirectory() throws IOException {
        File wrapper = Files.createFile(tempDir.resolve("gradlew")).toFile();
        Path subDir = Files.createDirectory(tempDir.resolve("subproject"));

        File result = IoUtils.getBuildToolWrapper(subDir, "gradlew", "gradlew.bat");

        assertThat(result).isNotNull().isEqualTo(wrapper);
    }

    // --- getExecutable ---

    @Test
    void getExecutableThrowsWhenNameIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.getExecutable(null));
    }

    @Test
    void getExecutableThrowsWhenNameIsEmpty() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.getExecutable(""));
    }

    // --- deleteSkillDirectory ---

    @Test
    void deleteSkillDirectoryDeletesValidSkillDirectory() throws IOException {
        // Set up: project/.agents/skills/my-skill/SKILL.md
        Path skillsBase = Files.createDirectories(tempDir.resolve(".agents/skills"));
        Path skillDir = Files.createDirectory(skillsBase.resolve("my-skill"));
        Files.writeString(skillDir.resolve("SKILL.md"), "---\nname: my-skill\n---\n# My Skill");
        Path scripts = Files.createDirectory(skillDir.resolve("scripts"));
        Files.writeString(scripts.resolve("run.sh"), "#!/bin/bash");

        IoUtils.deleteSkillDirectory(skillDir, tempDir, ".agents/skills");

        assertThat(skillDir).doesNotExist();
    }

    @Test
    void deleteSkillDirectoryRefusesPathOutsideSkillsDirectory() throws IOException {
        // A directory that is NOT under .agents/skills/
        Path outsideDir = Files.createDirectory(tempDir.resolve("not-skills"));
        Files.writeString(outsideDir.resolve("SKILL.md"), "---\nname: bad\n---");

        assertThatThrownBy(() ->
            IoUtils.deleteSkillDirectory(outsideDir, tempDir, ".agents/skills"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not inside the skills directory");
    }

    @Test
    void deleteSkillDirectoryRefusesDeeplyNestedPath() throws IOException {
        // A directory that is nested too deep: .agents/skills/a/b
        Path skillsBase = Files.createDirectories(tempDir.resolve(".agents/skills"));
        Path nested = Files.createDirectories(skillsBase.resolve("a/b"));
        Files.writeString(nested.resolve("SKILL.md"), "---\nname: bad\n---");

        assertThatThrownBy(() ->
            IoUtils.deleteSkillDirectory(nested, tempDir, ".agents/skills"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be a direct child");
    }

    @Test
    void deleteSkillDirectoryRefusesDirectoryWithoutSkillMd() throws IOException {
        // A valid path but no SKILL.md
        Path skillsBase = Files.createDirectories(tempDir.resolve(".agents/skills"));
        Path skillDir = Files.createDirectory(skillsBase.resolve("no-skill-md"));
        Files.writeString(skillDir.resolve("README.md"), "not a skill");

        assertThatThrownBy(() ->
            IoUtils.deleteSkillDirectory(skillDir, tempDir, ".agents/skills"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not contain SKILL.md");
    }

    // --- findTarGzFile ---

    @Test
    void findTarGzFileReturnsFileWhenPresent() throws IOException {
        Path tarGz = Files.createFile(tempDir.resolve("archive.tar.gz"));

        Path result = IoUtils.findTarGzFile(tempDir);

        assertThat(result).isEqualTo(tarGz);
    }

    @Test
    void findTarGzFileThrowsWhenNoTarGzFilePresent() {
        assertThatThrownBy(() -> IoUtils.findTarGzFile(tempDir))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(tempDir.toString());
    }

    @Test
    void findTarGzFileThrowsWhenDirectoryIsNull() {
        assertThatThrownBy(() -> IoUtils.findTarGzFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("directory cannot be null");
    }

    // --- createTempDirectory ---

    @Test
    void createTempDirectoryUnderParent() throws IOException {
        Path result = IoUtils.createTempDirectory(tempDir);
        assertThat(result).exists().isDirectory();
        assertThat(result.getParent()).isEqualTo(tempDir);
        assertThat(result.getFileName().toString()).startsWith(".arconia-tmp-");
    }

    @Test
    void createTempDirectoryCreatesParentIfNeeded() throws IOException {
        Path nestedParent = tempDir.resolve("a/b/c");
        assertThat(nestedParent).doesNotExist();

        Path result = IoUtils.createTempDirectory(nestedParent);
        assertThat(result).exists().isDirectory();
        assertThat(nestedParent).exists();
    }

    @Test
    void createTempDirectoryThrowsWhenParentIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.createTempDirectory(null))
                .withMessage("parentDirectory cannot be null");
    }

    // --- deleteTempDirectory ---

    @Test
    void deleteTempDirectoryDeletesDirectoryAndContents() throws IOException {
        Path child = IoUtils.createTempDirectory(tempDir);
        Files.writeString(child.resolve("file.txt"), "content");

        IoUtils.deleteTempDirectory(child);

        assertThat(child).doesNotExist();
    }

    @Test
    void deleteTempDirectoryNoOpWhenDirectoryDoesNotExist() throws IOException {
        Path nonExistent = tempDir.resolve(IoUtils.TEMP_DIR_PREFIX + "gone");

        // Should not throw
        IoUtils.deleteTempDirectory(nonExistent);
    }

    @Test
    void deleteTempDirectoryRefusesUnprefixedDirectory() throws IOException {
        Path unrelated = Files.createDirectories(tempDir.resolve("unrelated"));

        assertThatThrownBy(() -> IoUtils.deleteTempDirectory(unrelated))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not start with");
    }

    // --- discoverSubDirectoriesWithFile ---

    @Test
    void discoverSubDirectoriesWithFileThrowsWhenParentPathIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.discoverSubDirectoriesWithFile(null, "SKILL.md"));
    }

    @Test
    void discoverSubDirectoriesWithFileThrowsWhenFileNameIsEmpty() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.discoverSubDirectoriesWithFile(tempDir, ""));
    }

    @Test
    void discoverSubDirectoriesWithFileReturnsEmptyListWhenNoMatch() throws IOException {
        Files.createDirectory(tempDir.resolve("subdir"));

        List<Path> result = IoUtils.discoverSubDirectoriesWithFile(tempDir, "SKILL.md");

        assertThat(result).isEmpty();
    }

    @Test
    void discoverSubDirectoriesWithFileReturnsMatchingDirectory() throws IOException {
        Path subDir = Files.createDirectory(tempDir.resolve("my-skill"));
        Files.writeString(subDir.resolve("SKILL.md"), "content");

        List<Path> result = IoUtils.discoverSubDirectoriesWithFile(tempDir, "SKILL.md");

        assertThat(result).containsExactly(subDir);
    }

    @Test
    void discoverSubDirectoriesWithFileIgnoresFilesAtTopLevel() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), "content");

        List<Path> result = IoUtils.discoverSubDirectoriesWithFile(tempDir, "SKILL.md");

        assertThat(result).isEmpty();
    }

    @Test
    void discoverSubDirectoriesWithFileReturnsSortedResults() throws IOException {
        Path dirC = Files.createDirectory(tempDir.resolve("c-skill"));
        Path dirA = Files.createDirectory(tempDir.resolve("a-skill"));
        Path dirB = Files.createDirectory(tempDir.resolve("b-skill"));
        Files.writeString(dirA.resolve("SKILL.md"), "");
        Files.writeString(dirB.resolve("SKILL.md"), "");
        Files.writeString(dirC.resolve("SKILL.md"), "");

        List<Path> result = IoUtils.discoverSubDirectoriesWithFile(tempDir, "SKILL.md");

        assertThat(result).containsExactly(dirA, dirB, dirC);
    }

    // --- deleteEmptyDirectoriesRecursively ---

    @Test
    void deleteEmptyDirectoriesRecursivelyThrowsWhenDirectoryIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.deleteEmptyDirectoriesRecursively(null));
    }

    @Test
    void deleteEmptyDirectoriesRecursivelyIsNoOpForNonExistentPath() throws IOException {
        Path nonExistent = tempDir.resolve("does-not-exist");
        IoUtils.deleteEmptyDirectoriesRecursively(nonExistent);
        assertThat(nonExistent).doesNotExist();
    }

    @Test
    void deleteEmptyDirectoriesRecursivelyDeletesEmptyDirectory() throws IOException {
        Path emptyDir = Files.createDirectory(tempDir.resolve("empty"));

        IoUtils.deleteEmptyDirectoriesRecursively(emptyDir);

        assertThat(emptyDir).doesNotExist();
    }

    @Test
    void deleteEmptyDirectoriesRecursivelyDeletesNestedEmptyDirectories() throws IOException {
        Path nested = Files.createDirectories(tempDir.resolve("a/b/c"));

        IoUtils.deleteEmptyDirectoriesRecursively(tempDir.resolve("a"));

        assertThat(nested).doesNotExist();
        assertThat(tempDir.resolve("a")).doesNotExist();
    }

    @Test
    void deleteEmptyDirectoriesRecursivelyKeepsDirectoryWithFiles() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("has-file"));
        Files.writeString(dir.resolve("file.txt"), "content");

        IoUtils.deleteEmptyDirectoriesRecursively(dir);

        assertThat(dir).exists();
        assertThat(dir.resolve("file.txt")).exists();
    }

    @Test
    void deleteEmptyDirectoriesRecursivelyRemovesOnlyEmptyLeaves() throws IOException {
        Path parent = Files.createDirectories(tempDir.resolve("parent"));
        Path withFile = Files.createDirectory(parent.resolve("with-file"));
        Path empty = Files.createDirectory(parent.resolve("empty"));
        Files.writeString(withFile.resolve("file.txt"), "content");

        IoUtils.deleteEmptyDirectoriesRecursively(parent);

        assertThat(withFile).exists();
        assertThat(empty).doesNotExist();
    }

    // --- createTempFile ---

    @Test
    void createTempFileCreatesFileWithExpectedPrefixAndSuffix() throws IOException {
        Path result = IoUtils.createTempFile("my-prefix-", "-my-suffix.txt");

        assertThat(result).exists().isRegularFile();
        assertThat(result.getFileName().toString()).startsWith("my-prefix-").endsWith("-my-suffix.txt");
        result.toFile().deleteOnExit();
    }

    @Test
    void createTempFileIsWritable() throws IOException {
        Path result = IoUtils.createTempFile("test-", ".tmp");

        assertThat(result).isWritable();
        result.toFile().deleteOnExit();
    }

    // --- createSkillSymlink ---

    @Test
    void createSkillSymlinkThrowsWhenLinkIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.createSkillSymlink(null, tempDir.resolve("target")));
    }

    @Test
    void createSkillSymlinkThrowsWhenTargetIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.createSkillSymlink(tempDir.resolve("link"), null));
    }

    @Test
    void createSkillSymlinkCreatesParentDirectories() throws IOException {
        Path target = Files.createDirectory(tempDir.resolve("target"));
        Path link = tempDir.resolve("new-parent/link");

        IoUtils.createSkillSymlink(link, target);

        assertThat(link.getParent()).exists();
    }

    @Test
    void createSkillSymlinkReplacesExistingSymlink() throws IOException {
        Path target1 = Files.createDirectory(tempDir.resolve("target1"));
        Path target2 = Files.createDirectory(tempDir.resolve("target2"));
        Path link = tempDir.resolve("link");
        Files.createSymbolicLink(link, target1);

        IoUtils.createSkillSymlink(link, target2);

        assertThat(link).exists();
        assertThat(Files.isSymbolicLink(link)).isTrue();
    }

    // --- deleteSymlinkOrDirectory ---

    @Test
    void deleteSymlinkOrDirectoryThrowsWhenPathIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.deleteSymlinkOrDirectory(null));
    }

    @Test
    void deleteSymlinkOrDirectoryIsNoOpWhenPathDoesNotExist() throws IOException {
        Path nonExistent = tempDir.resolve("does-not-exist");
        IoUtils.deleteSymlinkOrDirectory(nonExistent);
        assertThat(nonExistent).doesNotExist();
    }

    @Test
    void deleteSymlinkOrDirectoryDeletesSymlink() throws IOException {
        Path target = Files.createDirectory(tempDir.resolve("target"));
        Path link = tempDir.resolve("link");
        Files.createSymbolicLink(link, target);

        IoUtils.deleteSymlinkOrDirectory(link);

        assertThat(Files.exists(link, java.nio.file.LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(target).exists();
    }

    @Test
    void deleteSymlinkOrDirectoryDeletesDirectoryRecursively() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("dir"));
        Files.writeString(dir.resolve("file.txt"), "content");

        IoUtils.deleteSymlinkOrDirectory(dir);

        assertThat(dir).doesNotExist();
    }

    // --- deriveSkillsBasePath ---

    @Test
    void deriveSkillsBasePathThrowsWhenSkillPathIsEmpty() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> IoUtils.deriveSkillsBasePath(""));
    }

    @Test
    void deriveSkillsBasePathThrowsWhenNoParentDirectory() {
        assertThatThrownBy(() -> IoUtils.deriveSkillsBasePath("no-parent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no parent directory");
    }

    @Test
    void deriveSkillsBasePathReturnsParentPath() {
        assertThat(IoUtils.deriveSkillsBasePath(".agents/skills/my-skill"))
                .isEqualTo(".agents/skills");
    }

    @Test
    void deriveSkillsBasePathWorksForDeeperPath() {
        assertThat(IoUtils.deriveSkillsBasePath(".claude/skills/pull-request"))
                .isEqualTo(".claude/skills");
    }

    // --- copyFileToTemp ---

    @Test
    void copyFileToTempCopiesExistingClasspathResource() throws IOException {
        Path result = IoUtils.copyFileToTemp("openrewrite/init-rewrite.gradle");

        assertThat(result).exists().isReadable();
        assertThat(Files.readString(result)).contains("exportDatatables");
    }

    @Test
    void copyFileToTempCreatesFileWithExpectedSuffix() throws IOException {
        Path result = IoUtils.copyFileToTemp("openrewrite/init-rewrite.gradle");

        assertThat(result.getFileName().toString()).endsWith("init-rewrite.gradle");
    }

    @Test
    void copyFileToTempThrowsForNonExistentResource() {
        assertThatThrownBy(() -> IoUtils.copyFileToTemp("nonexistent/file.gradle"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("nonexistent/file.gradle");
    }

}
