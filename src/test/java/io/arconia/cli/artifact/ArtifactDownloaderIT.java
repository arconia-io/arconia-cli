package io.arconia.cli.artifact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import land.oras.Annotations;
import land.oras.ArtifactType;
import land.oras.ContainerRef;
import land.oras.Registry;

import io.arconia.cli.OciIntegrationTests;
import io.arconia.cli.project.oci.ProjectConfig;
import io.arconia.cli.project.oci.ProjectMediaTypes;
import io.arconia.cli.utils.IoUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ArtifactDownloader}.
 */
class ArtifactDownloaderIT extends OciIntegrationTests {

    private static final String ARTIFACT_NAME = "my-artifact";
    private static final String OCI_REFERENCE_TAG = "1.0.0";

    @TempDir
    Path sourceDir;

    @TempDir
    Path downloadDir;

    private ArtifactDownloader downloader;
    private String ociReference;

    @BeforeEach
    void setUp() throws IOException {
        String registryUrl = zotContainer.getHost() + ":" + zotContainer.getMappedPort(5000);
        ociReference = "%s/test/%s:%s".formatted(registryUrl, ARTIFACT_NAME, OCI_REFERENCE_TAG);

        Registry registry = Registry.builder()
                .withRegistry(registryUrl)
                .withInsecure(true)
                .withSkipTlsVerify(true)
                .build();

        // Create source content and publish it
        Files.writeString(sourceDir.resolve("README.md"), "# Test artifact");
        ArtifactPublisher publisher = new ArtifactPublisher(registry);
        publisher.publish(ArtifactInfo.builder()
                .config(ProjectConfig.builder().name("test-app").description("A test application").build().toConfig())
                .annotations(Annotations.empty())
                .path(sourceDir)
                .containerRef(ContainerRef.parse(ociReference))
                .artifactType(ArtifactType.from(ProjectMediaTypes.PROJECT_ARTIFACT_TYPE))
                .contentType(ProjectMediaTypes.PROJECT_ARTIFACT_CONTENT_LAYER)
                .build());

        downloader = new ArtifactDownloader(registry);
    }

    @Test
    void downloadReturnsTargetDirectoryWithOriginalFiles() throws IOException {
        Path result = downloader.download(ARTIFACT_NAME, ociReference, downloadDir);

        assertThat(result).isEqualTo(downloadDir.resolve(ARTIFACT_NAME));
        assertThat(result).isDirectory();

        assertThat(result.resolve("README.md")).exists();
        assertThat(result.resolve("README.md")).hasContent("# Test artifact");
    }

    @Test
    void downloadCleansUpTempDirectories() throws IOException {
        downloader.download(ARTIFACT_NAME, ociReference, downloadDir);

        try (var entries = Files.list(downloadDir)) {
            assertThat(entries.filter(p -> p.getFileName().toString().startsWith(IoUtils.TEMP_DIR_PREFIX)))
                    .isEmpty();
        }
    }

}
