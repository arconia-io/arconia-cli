package io.arconia.cli.project.catalog;

import java.util.List;

import org.springframework.util.Assert;

/**
 * Represents a single entry in a project catalog.
 */
public record ProjectCatalogEntry(
        String name,
        String ref,
        String tag,
        String digest,
        String description,
        String type,
        List<String> labels
) {

    public ProjectCatalogEntry {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(ref, "ref cannot be null or empty");
        Assert.hasText(tag, "tag cannot be null or empty");
        Assert.hasText(digest, "digest cannot be null or empty");
        Assert.hasText(description, "description cannot be null or empty");
        Assert.hasText(type, "type cannot be null or empty");
        Assert.notNull(labels, "labels cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String tag;
        private String ref;
        private String digest;
        private String description;
        private String type;
        private List<String> labels;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder ref(String ref) {
            this.ref = ref;
            return this;
        }

        public Builder digest(String digest) {
            this.digest = digest;
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

        public ProjectCatalogEntry build() {
            return new ProjectCatalogEntry(name, ref, tag, digest, description, type, labels);
        }

    }

}
