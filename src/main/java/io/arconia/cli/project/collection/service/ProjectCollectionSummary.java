package io.arconia.cli.project.collection.service;

import java.util.List;

import org.springframework.util.Assert;

/**
 * Represents a summary of a project collection.
 */
public record ProjectCollectionSummary(
        String name,
        String description,
        String type,
        List<String> labels,
        String version,
        String ref
) {

    public ProjectCollectionSummary {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(description, "description cannot be null or empty");
        Assert.hasText(type, "type cannot be null or empty");
        Assert.notNull(labels, "labels cannot be null");
        Assert.hasText(version, "version cannot be null or empty");
        Assert.hasText(ref, "ref cannot be null or empty");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private String type;
        private List<String> labels;
        private String version;
        private String ref;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder labels(List<String> labels) {
            this.labels = labels;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder ref(String ref) {
            this.ref = ref;
            return this;
        }

        public ProjectCollectionSummary build() {
            return new ProjectCollectionSummary(name, description, type, labels, version, ref);
        }

    }

}
