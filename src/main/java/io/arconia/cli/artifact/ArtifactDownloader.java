package io.arconia.cli.artifact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import land.oras.ContainerRef;
import land.oras.Registry;
import land.oras.utils.ArchiveUtils;
import land.oras.utils.SupportedCompression;

import io.arconia.cli.utils.IoUtils;

import org.springframework.util.Assert;

/**
 * Responsible for downloading OCI artifacts from an OCI registry.
 */
public final class ArtifactDownloader {

    private final Registry registry;

    public ArtifactDownloader(Registry registry) {
        Assert.notNull(registry, "registry cannot be null");
        this.registry = registry;
    }

    /**
     * Downloads an OCI artifact from a registry and extracts it to a local directory.
     */
    public Path download(String artifactName, String ociReference, Path targetParentDirectory) throws IOException {
        Assert.hasText(artifactName, "artifactName cannot be null or empty");
        Assert.hasText(ociReference, "ociReference cannot be null or empty");
        Assert.notNull(targetParentDirectory, "targetParentDirectory cannot be null");

        ContainerRef containerRef = ContainerRef.parse(ociReference);
        Path targetDirectory = targetParentDirectory.resolve(artifactName);

        Path tempDir = IoUtils.createTempDirectory(targetParentDirectory);
        try {
            registry.pullArtifact(containerRef, tempDir, true);
            Path tarGzFile = IoUtils.findTarGzFile(tempDir);
            ArchiveUtils.uncompressuntar(tarGzFile, tempDir, SupportedCompression.GZIP.getMediaType());

            try (Stream<Path> entries = Files.list(tempDir)) {
                Path extractedDir = entries
                        .filter(Files::isDirectory)
                        .findFirst()
                        .orElseThrow(() -> new IOException("No directory found in downloaded artifact"));
                Files.move(extractedDir, targetDirectory);
            }
        }
        finally {
            IoUtils.deleteTempDirectory(tempDir);
        }

        return targetDirectory;
    }

}
