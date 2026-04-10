package io.arconia.cli.project.catalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.util.Assert;

import io.arconia.cli.json.JsonParser;

/**
 * Model for a report generated when a project catalog is pushed to a registry.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectCatalogPushReport(
    List<ArtifactEntry> artifacts
) {

    public static final String DEFAULT_FILENAME = "template-catalog-push-report.json";

    public ProjectCatalogPushReport {
        Assert.notNull(artifacts, "artifacts cannot be null");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtifactEntry(
        String name,
        String description,
        String ref,
        String tag,
        String digest
    ) {

        public ArtifactEntry {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.hasText(description, "description cannot be null or empty");
            Assert.hasText(ref, "ref cannot be null or empty");
            Assert.hasText(tag, "tag cannot be null or empty");
            Assert.hasText(digest, "digest cannot be null or empty");
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;
            private String description;
            private String ref;
            private String tag;
            private String digest;

            private Builder() {}

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder ref(String ref) {
                this.ref = ref;
                return this;
            }

            public Builder tag(String tag) {
                this.tag = tag;
                return this;
            }

            public Builder digest(String digest) {
                this.digest = digest;
                return this;
            }

            public ArtifactEntry build() {
                return new ArtifactEntry(name, description, ref, tag, digest);
            }

        }

    }

    public void save(Path path) throws IOException {
        Assert.notNull(path, "path cannot be null");

        String json = JsonParser.toJsonPrettyPrint(this);
        Files.writeString(path, json + "\n");
    }

    public static ProjectCatalogPushReport load(Path path) throws IOException {
        Assert.notNull(path, "path cannot be null");

        String json = Files.readString(path);
        return JsonParser.fromJson(json, ProjectCatalogPushReport.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<ArtifactEntry> artifacts = new ArrayList<>();

        private Builder() {}

        public Builder artifacts(List<ArtifactEntry> artifacts) {
            this.artifacts = new ArrayList<>(artifacts);
            return this;
        }

        public Builder artifact(ArtifactEntry artifact) {
            Assert.notNull(artifact, "artifact cannot be null");
            this.artifacts.add(artifact);
            return this;
        }

        public ProjectCatalogPushReport build() {
            return new ProjectCatalogPushReport(artifacts);
        }

    }

}
