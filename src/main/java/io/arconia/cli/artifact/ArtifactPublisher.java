package io.arconia.cli.artifact;

import org.springframework.util.Assert;

import land.oras.LocalPath;
import land.oras.Manifest;
import land.oras.Registry;
import land.oras.utils.ArchiveUtils;
import land.oras.utils.SupportedCompression;

/**
 * Responsible for publishing OCI artifacts to an OCI registry.
 */
public class ArtifactPublisher {

    private final Registry registry;

    public ArtifactPublisher(Registry registry) {
        Assert.notNull(registry, "registry cannot be null");
        this.registry = registry;
    }

    /**
     * Publish an OCI artifact to the registry.
     */
    public Manifest publish(ArtifactInfo artifactInfo) {
        Assert.notNull(artifactInfo, "artifactInfo cannot be null");

        LocalPath artifactPath = LocalPath.of(artifactInfo.path(), artifactInfo.contentType());
        LocalPath contentLayer = ArchiveUtils.tarcompress(artifactPath, SupportedCompression.GZIP.getMediaType());
        contentLayer = LocalPath.of(contentLayer.getPath(), artifactInfo.contentType());

        return registry.pushArtifact(
                artifactInfo.containerRef(),
                artifactInfo.artifactType(),
                artifactInfo.annotations(),
                artifactInfo.config(),
                contentLayer);
    }

}
