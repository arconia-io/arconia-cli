package io.arconia.cli.image;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import land.oras.ContainerRef;
import land.oras.Index;
import land.oras.Manifest;
import land.oras.ManifestDescriptor;
import land.oras.Platform;
import land.oras.Registry;

import io.arconia.cli.commands.options.OutputOptions;

/**
 * Creates and pushes an OCI Image Index (manifest list) to an OCI-compliant registry
 * for multi-architecture container image builds.
 */
public final class MultiArchImagePublisher {

    private final Registry registry;
    private final OutputOptions outputOptions;

    public MultiArchImagePublisher(Registry registry, OutputOptions outputOptions) {
        Assert.notNull(registry, "registry cannot be null");
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        this.registry = registry;
        this.outputOptions = outputOptions;
    }

    /**
     * Create and push an OCI Image Index that references the per-platform images.
     */
    public Index publish(String baseImageName, List<PlatformImage> platformImages) {
        Assert.hasText(baseImageName, "baseImageName must not be null or empty");
        Assert.notEmpty(platformImages, "platformImages must not be null or empty");

        outputOptions.info("Creating multi-arch manifest index for '%s'...".formatted(baseImageName));

        List<ManifestDescriptor> descriptors = new ArrayList<>();

        for (PlatformImage platformImage : platformImages) {
            outputOptions.verbose("Fetching manifest for '%s' (%s)...".formatted(
                    platformImage.imageRef(), platformImage.platformString()));

            ContainerRef containerRef = ContainerRef.parse(platformImage.imageRef());
            Manifest manifest = registry.getManifest(containerRef);

            Platform platform = ImagePlatformUtils.parsePlatform(platformImage.platformString());

            ManifestDescriptor descriptor = manifest.getDescriptor()
                    .withPlatform(platform);

            descriptors.add(descriptor);

            outputOptions.verbose("  Digest: %s, Platform: %s/%s".formatted(
                    manifest.getDescriptor().getDigest(),
                    platform.os(),
                    platform.architecture()));
        }

        ContainerRef indexRef = ContainerRef.parse(baseImageName);
        Index index = Index.fromManifests(descriptors);
        Index pushedIndex = registry.pushIndex(indexRef, index);

        outputOptions.info("Pushed multi-arch manifest index: %s".formatted(baseImageName));
        outputOptions.info("Digest: %s".formatted(pushedIndex.getDescriptor().getDigest()));

        return pushedIndex;
    }

    /**
     * Represents a per-platform image that has been built and pushed.
     */
    public record PlatformImage(String imageRef, String platformString) {

        public PlatformImage {
            Assert.hasText(imageRef, "imageRef must not be null or empty");
            Assert.hasText(platformString, "platformString must not be null or empty");
        }

    }

}
