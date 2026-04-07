package io.arconia.cli.project.collection.service;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Represents the header of a project collection.
 */
public record ProjectCollectionHeader(
        String name,
        String ref,
        @Nullable String description
) {

    public ProjectCollectionHeader {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(ref, "ref cannot be null or empty");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String ref;
        private @Nullable String description;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder ref(String ref) {
            this.ref = ref;
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public ProjectCollectionHeader build() {
            return new ProjectCollectionHeader(name, ref, description);
        }

    }

}
