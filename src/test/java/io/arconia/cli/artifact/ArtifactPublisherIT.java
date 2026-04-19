package io.arconia.cli.artifact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import land.oras.Annotations;
import land.oras.ArtifactType;
import land.oras.ContainerRef;
import land.oras.Manifest;
import land.oras.Registry;

import io.arconia.cli.OciIntegrationTests;
import io.arconia.cli.project.oci.ProjectConfig;
import io.arconia.cli.project.oci.ProjectMediaTypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link ArtifactPublisher}.
 */
class ArtifactPublisherIT extends OciIntegrationTests {

    @TempDir
    Path tempDir;

    private ArtifactPublisher publisher;
    private String registryUrl;

    @BeforeEach
    void setUp() throws IOException {
        registryUrl = zotContainer.getHost() + ":" + zotContainer.getMappedPort(5000);
        publisher = new ArtifactPublisher(Registry.builder()
                .withRegistry(registryUrl)
                .withInsecure(true)
                .withSkipTlsVerify(true)
                .build());
        Files.writeString(tempDir.resolve("README.md"), "# Test artifact");
    }

    @Test
    void publishArtifactReturnsManifest() {
        Manifest manifest = publisher.publish(buildArtifactInfo("test-app", "1.0.0", Annotations.empty()));

        assertThat(manifest).isNotNull();
        assertThat(manifest.getDigest()).startsWith("sha256:");
        assertThat(manifest.getArtifactTypeAsString()).isEqualTo(ProjectMediaTypes.PROJECT_ARTIFACT_TYPE);
        assertThat(manifest.getConfig().getMediaType()).isEqualTo(ProjectMediaTypes.PROJECT_ARTIFACT_CONFIG);

        assertThat(manifest.getLayers()).hasSize(1);
        assertThat(manifest.getLayers().getFirst().getMediaType()).isEqualTo(ProjectMediaTypes.PROJECT_ARTIFACT_CONTENT_LAYER);
        assertThat(manifest.getLayers().getFirst().getDigest()).startsWith("sha256:");
        assertThat(manifest.getLayers().getFirst().getSize()).isPositive();
    }

    @Test
    void publishReturnsManifestWithAnnotations() {
        Annotations annotations = Annotations.ofManifest(Map.of(ArtifactAnnotations.OCI_TITLE, "Test App"));
        Manifest manifest = publisher.publish(buildArtifactInfo("test-app-annotated", "1.0.0", annotations));

        assertThat(manifest.getAnnotations()).containsEntry(ArtifactAnnotations.OCI_TITLE, "Test App");
    }

    @Test
    void publishThrowsForNullArtifactInfo() {
        assertThatThrownBy(() -> publisher.publish(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifactInfo cannot be null");
    }

    private ArtifactInfo buildArtifactInfo(String repo, String tag, Annotations annotations) {
        ProjectConfig config = ProjectConfig.builder()
                .name("test-app")
                .description("A test application")
                .build();

        return ArtifactInfo.builder()
                .config(config.toConfig())
                .annotations(annotations)
                .path(tempDir)
                .containerRef(ContainerRef.parse("%s/test/%s:%s".formatted(registryUrl, repo, tag)))
                .artifactType(ArtifactType.from(ProjectMediaTypes.PROJECT_ARTIFACT_TYPE))
                .contentType(ProjectMediaTypes.PROJECT_ARTIFACT_CONTENT_LAYER)
                .build();
    }

}
