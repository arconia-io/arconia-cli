package io.arconia.cli.artifact;

import java.nio.file.Path;

import org.springframework.util.Assert;

import land.oras.Annotations;
import land.oras.ArtifactType;
import land.oras.Config;
import land.oras.ContainerRef;

/**
 * Holds information about an OCI artifact.
 */
public record ArtifactInfo(
        Config config,
        Annotations annotations,
        Path path,
        ContainerRef containerRef,
        ArtifactType artifactType,
        String contentType
) {

    public ArtifactInfo {
        Assert.notNull(config, "config cannot be null");
        Assert.notNull(annotations, "annotations cannot be null");
        Assert.notNull(path, "path cannot be null");
        Assert.notNull(containerRef, "containerRef cannot be null");
        Assert.notNull(artifactType, "artifactType cannot be null");
        Assert.hasText(contentType, "contentType cannot be null or empty");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Config config;
        private Annotations annotations;
        private Path path;
        private ContainerRef containerRef;
        private ArtifactType artifactType;
        private String contentType;

        private Builder() {}

        public Builder config(Config config) {
            this.config = config;
            return this;
        }

        public Builder annotations(Annotations annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder path(Path path) {
            this.path = path;
            return this;
        }

        public Builder containerRef(ContainerRef containerRef) {
            this.containerRef = containerRef;
            return this;
        }

        public Builder artifactType(ArtifactType artifactType) {
            this.artifactType = artifactType;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public ArtifactInfo build() {
            return new ArtifactInfo(config, annotations, path, containerRef, artifactType, contentType);
        }

    }

}
