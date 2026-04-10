package io.arconia.cli.project;

import jakarta.annotation.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Gathers all the information needed to create a new project.
 */
public record ProjectCreateArguments(
        String name,
        String description,
        String group,
        String packageName
) {

    public ProjectCreateArguments {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(description, "description cannot be null or empty");
        Assert.hasText(group, "group cannot be null or empty");
        Assert.hasText(packageName, "packageName cannot be null or empty");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        @Nullable private String name;
        @Nullable private String description;
        @Nullable private String group;
        @Nullable private String packageName;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder group(@Nullable String group) {
            this.group = group;
            return this;
        }

        public Builder packageName(@Nullable String packageName) {
            this.packageName = packageName;
            return this;
        }

        public ProjectCreateArguments build() {
            if (!StringUtils.hasText(description)) {
                description = name;
            }

            if (!StringUtils.hasText(packageName)) {
                packageName = group + "." + name;
            }

            return new ProjectCreateArguments(name, description, group, packageName);
        }

    }

}
