package io.arconia.cli.artifact;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import land.oras.Annotations;
import land.oras.ArtifactType;
import land.oras.Config;
import land.oras.ContainerRef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ArtifactInfo}.
 */
class ArtifactInfoTests {

    private static final Config CONFIG = Config.empty();
    private static final Annotations ANNOTATIONS = Annotations.empty();
    private static final Path PATH = Path.of("/tmp/artifact.tar.gz");
    private static final ContainerRef CONTAINER_REF = ContainerRef.parse("ghcr.io/org/repo:1.0.0");
    private static final ArtifactType ARTIFACT_TYPE = ArtifactType.from("application/vnd.test.v1");
    private static final String CONTENT_TYPE = "application/vnd.test.layer.v1.tar+gzip";

    @Test
    void buildsSuccessfullyWithAllFields() {
        ArtifactInfo info = ArtifactInfo.builder()
                .config(CONFIG)
                .annotations(ANNOTATIONS)
                .path(PATH)
                .containerRef(CONTAINER_REF)
                .artifactType(ARTIFACT_TYPE)
                .contentType(CONTENT_TYPE)
                .build();

        assertThat(info.config()).isEqualTo(CONFIG);
        assertThat(info.annotations()).isEqualTo(ANNOTATIONS);
        assertThat(info.path()).isEqualTo(PATH);
        assertThat(info.containerRef()).isEqualTo(CONTAINER_REF);
        assertThat(info.artifactType()).isEqualTo(ARTIFACT_TYPE);
        assertThat(info.contentType()).isEqualTo(CONTENT_TYPE);
    }

    @Test
    void throwsWhenConfigIsNull() {
        assertThatThrownBy(() -> ArtifactInfo.builder()
                .annotations(ANNOTATIONS)
                .path(PATH)
                .containerRef(CONTAINER_REF)
                .artifactType(ARTIFACT_TYPE)
                .contentType(CONTENT_TYPE)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("config cannot be null");
    }

    @Test
    void throwsWhenAnnotationsIsNull() {
        assertThatThrownBy(() -> ArtifactInfo.builder()
                .config(CONFIG)
                .path(PATH)
                .containerRef(CONTAINER_REF)
                .artifactType(ARTIFACT_TYPE)
                .contentType(CONTENT_TYPE)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("annotations cannot be null");
    }

    @Test
    void throwsWhenPathIsNull() {
        assertThatThrownBy(() -> ArtifactInfo.builder()
                .config(CONFIG)
                .annotations(ANNOTATIONS)
                .containerRef(CONTAINER_REF)
                .artifactType(ARTIFACT_TYPE)
                .contentType(CONTENT_TYPE)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path cannot be null");
    }

    @Test
    void throwsWhenContainerRefIsNull() {
        assertThatThrownBy(() -> ArtifactInfo.builder()
                .config(CONFIG)
                .annotations(ANNOTATIONS)
                .path(PATH)
                .artifactType(ARTIFACT_TYPE)
                .contentType(CONTENT_TYPE)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("containerRef cannot be null");
    }

    @Test
    void throwsWhenArtifactTypeIsNull() {
        assertThatThrownBy(() -> ArtifactInfo.builder()
                .config(CONFIG)
                .annotations(ANNOTATIONS)
                .path(PATH)
                .containerRef(CONTAINER_REF)
                .contentType(CONTENT_TYPE)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifactType cannot be null");
    }

    @Test
    void throwsWhenContentTypeIsNull() {
        assertThatThrownBy(() -> ArtifactInfo.builder()
                .config(CONFIG)
                .annotations(ANNOTATIONS)
                .path(PATH)
                .containerRef(CONTAINER_REF)
                .artifactType(ARTIFACT_TYPE)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contentType cannot be null or empty");
    }

    @Test
    void throwsWhenContentTypeIsEmpty() {
        assertThatThrownBy(() -> ArtifactInfo.builder()
                .config(CONFIG)
                .annotations(ANNOTATIONS)
                .path(PATH)
                .containerRef(CONTAINER_REF)
                .artifactType(ARTIFACT_TYPE)
                .contentType("")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contentType cannot be null or empty");
    }

}
