package io.arconia.cli.image;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import land.oras.Annotations;
import land.oras.ArtifactType;
import land.oras.Config;
import land.oras.ContainerRef;
import land.oras.Index;
import land.oras.LocalPath;
import land.oras.ManifestDescriptor;
import land.oras.Registry;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import io.arconia.cli.OciIntegrationTests;
import io.arconia.cli.commands.options.OutputOptions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MultiArchImagePublisher}.
 */
class MultiArchImagePublisherIT extends OciIntegrationTests {

    @TempDir
    Path tempDir;

    private MultiArchImagePublisher publisher;
    private Registry registry;
    private String registryUrl;

    @BeforeEach
    void setUp() {
        registryUrl = zotContainer.getHost() + ":" + zotContainer.getMappedPort(5000);
        registry = Registry.builder()
                .withRegistry(registryUrl)
                .withInsecure(true)
                .withSkipTlsVerify(true)
                .build();
        publisher = new MultiArchImagePublisher(registry, createOutputOptions());
    }

    @Test
    void publishCreatesMultiArchIndex() throws IOException {
        // Push two simulated per-platform manifests
        String amd64Ref = pushSimulatedImage("multi-arch-test", "1.0-amd64");
        String arm64Ref = pushSimulatedImage("multi-arch-test", "1.0-arm64");

        String baseImageName = "%s/test/multi-arch-test:1.0".formatted(registryUrl);

        List<MultiArchImagePublisher.PlatformImage> platformImages = List.of(
                new MultiArchImagePublisher.PlatformImage(amd64Ref, "linux/amd64"),
                new MultiArchImagePublisher.PlatformImage(arm64Ref, "linux/arm64")
        );

        Index index = publisher.publish(baseImageName, platformImages);

        assertThat(index).isNotNull();
        assertThat(index.getDescriptor().getDigest()).startsWith("sha256:");
        assertThat(index.getManifests()).hasSize(2);

        // Verify the index is fetchable from the registry
        Index fetched = registry.getIndex(ContainerRef.parse(baseImageName));
        assertThat(fetched.getManifests()).hasSize(2);

        // Verify platform information is set on descriptors
        List<ManifestDescriptor> manifests = fetched.getManifests();
        assertThat(manifests).anySatisfy(m -> {
            assertThat(m.getPlatform().os()).isEqualTo("linux");
            assertThat(m.getPlatform().architecture()).isEqualTo("amd64");
        });
        assertThat(manifests).anySatisfy(m -> {
            assertThat(m.getPlatform().os()).isEqualTo("linux");
            assertThat(m.getPlatform().architecture()).isEqualTo("arm64");
        });
    }

    @Test
    void publishedIndexDigestIsConsistent() throws IOException {
        String ref1 = pushSimulatedImage("digest-test", "1.0-amd64");
        String ref2 = pushSimulatedImage("digest-test", "1.0-arm64");

        String baseImageName = "%s/test/digest-test:1.0".formatted(registryUrl);

        List<MultiArchImagePublisher.PlatformImage> platformImages = List.of(
                new MultiArchImagePublisher.PlatformImage(ref1, "linux/amd64"),
                new MultiArchImagePublisher.PlatformImage(ref2, "linux/arm64")
        );

        Index index = publisher.publish(baseImageName, platformImages);

        // Fetch and verify digest matches
        Index fetched = registry.getIndex(ContainerRef.parse(baseImageName));
        assertThat(fetched.getDescriptor().getDigest()).isEqualTo(index.getDescriptor().getDigest());
    }

    /**
     * Push a minimal simulated OCI artifact manifest to the registry.
     * Returns the fully-qualified image reference.
     */
    private String pushSimulatedImage(String name, String tag) throws IOException {
        ContainerRef ref = ContainerRef.parse("%s/test/%s:%s".formatted(registryUrl, name, tag));

        // Create a temporary file to serve as the artifact layer
        Path layerFile = tempDir.resolve("%s-%s.txt".formatted(name, tag));
        Files.writeString(layerFile, "simulated content for %s:%s".formatted(name, tag));

        // Push a minimal artifact with one layer file (this properly pushes config + layer + manifest)
        registry.pushArtifact(ref, ArtifactType.unknown(), Annotations.empty(), (Config) null,
                LocalPath.of(layerFile));

        return ref.toString();
    }

    private static OutputOptions createOutputOptions() {
        TestCommand testCommand = new TestCommand();
        new CommandLine(testCommand)
                .setOut(new PrintWriter(new StringWriter()))
                .setErr(new PrintWriter(new StringWriter()))
                .parseArgs();
        return testCommand.outputOptions;
    }

    @Command(name = "test")
    private static class TestCommand implements Runnable {
        @Mixin
        OutputOptions outputOptions = new OutputOptions();

        @Override
        public void run() {}
    }

}
