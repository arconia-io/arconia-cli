package io.arconia.cli.utils;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GitUtils}.
 */
class GitUtilsTests {

    @TempDir
    Path tempDir;

    @Test
    void getRevisionReturnsShortShaInGitRepo() {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        String revision = GitUtils.getRevision(projectRoot);
        assertThat(revision)
            .isNotNull()
            .matches("[0-9a-f]{7,12}");
    }

    @Test
    void getRevisionReturnsNullForNonGitDirectory() {
        String revision = GitUtils.getRevision(tempDir);
        assertThat(revision).isNull();
    }

    @Test
    void getRevisionWithNullDefaultsToWorkingDirectory() {
        String revision = GitUtils.getRevision(null);
        assertThat(revision)
            .isNotNull()
            .matches("[0-9a-f]{7,12}");
    }

    @Test
    void getRemoteUrlReturnsUrlInGitRepo() {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        String remoteUrl = GitUtils.getRemoteUrl(projectRoot);
        assertThat(remoteUrl)
            .isNotNull()
            .contains("arconia-cli");
    }

    @Test
    void getRemoteUrlReturnsNullForNonGitDirectory() {
        String remoteUrl = GitUtils.getRemoteUrl(tempDir);
        assertThat(remoteUrl).isNull();
    }

    @Test
    void getRemoteUrlWithNullDefaultsToWorkingDirectory() {
        String remoteUrl = GitUtils.getRemoteUrl(null);
        assertThat(remoteUrl)
            .isNotNull()
            .contains("arconia-cli");
    }

}
