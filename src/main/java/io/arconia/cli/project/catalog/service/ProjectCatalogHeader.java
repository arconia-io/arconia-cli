package io.arconia.cli.project.catalog.service;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Represents the header of a project catalog.
 */
public record ProjectCatalogHeader(
        String name,
        String ref,
        @Nullable String description
) {

    public ProjectCatalogHeader {
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

        public ProjectCatalogHeader build() {
            return new ProjectCatalogHeader(name, ref, description);
        }

    }

}
