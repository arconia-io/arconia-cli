package io.arconia.cli.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        assertThat(result.toString()).isEqualTo(
                Path.of(System.getProperty("user.dir")).toAbsolutePath().toString());
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
