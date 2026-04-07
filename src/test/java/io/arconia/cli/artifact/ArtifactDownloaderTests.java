package io.arconia.cli.artifact;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import land.oras.Registry;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ArtifactDownloader}.
 */
class ArtifactDownloaderTests {

    private final ArtifactDownloader artifactDownloader = new ArtifactDownloader(Registry.builder().build());

    @Test
    void whenRegistryIsNullThenThrow() {
        assertThatThrownBy(() -> new ArtifactDownloader(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registry cannot be null");
    }

    @Test
    void whenArtifactNameIsNullThenThrow() {
        assertThatThrownBy(() -> artifactDownloader.download(null, "ghcr.io/org/repo:1.0.0", Path.of("/tmp")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifactName cannot be null or empty");
    }

    @Test
    void whenArtifactNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> artifactDownloader.download("", "ghcr.io/org/repo:1.0.0", Path.of("/tmp")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifactName cannot be null or empty");
    }

    @Test
    void whenOciReferenceIsNullThenThrow() {
        assertThatThrownBy(() -> artifactDownloader.download("my-artifact", null, Path.of("/tmp")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ociReference cannot be null or empty");
    }

    @Test
    void whenOciReferenceIsEmptyThenThrow() {
        assertThatThrownBy(() -> artifactDownloader.download("my-artifact", "", Path.of("/tmp")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ociReference cannot be null or empty");
    }

    @Test
    void whenTargetParentDirectoryIsNullThenThrow() {
        assertThatThrownBy(() -> artifactDownloader.download("my-artifact", "ghcr.io/org/repo:1.0.0", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetParentDirectory cannot be null");
    }

}
