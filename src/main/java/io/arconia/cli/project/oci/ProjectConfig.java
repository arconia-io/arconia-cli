package io.arconia.cli.project.oci;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import land.oras.Config;

import io.arconia.cli.artifact.ArtifactConfigParser;

/**
 * The OCI Config object embedded in a Project Artifact manifest.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectConfig(
    String schemaVersion,
    String name,
    String description,
    String type,
    String license,
    String packageName,
    List<String> labels
) {

    public static final String CURRENT_SCHEMA_VERSION = "1";
    public static final String DEFAULT_TYPE = "application";
    public static final String DEFAULT_LICENSE = "LicenseRef-Proprietary";
    public static final String DEFAULT_PACKAGE_NAME = "com.example";

    public ProjectConfig {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(description, "description cannot be null or empty");

        if (!StringUtils.hasText(schemaVersion)) {
            schemaVersion = CURRENT_SCHEMA_VERSION;
        }
        if (!StringUtils.hasText(type)) {
            type = DEFAULT_TYPE;
        }
        if (!StringUtils.hasText(license)) {
            license = DEFAULT_LICENSE;
        }
        if (!StringUtils.hasText(packageName)) {
            packageName = DEFAULT_PACKAGE_NAME;
        }
        if (CollectionUtils.isEmpty(labels)) {
            labels = List.of();
        }
    }

    public Config toConfig() {
        return ArtifactConfigParser.fromObject(this, ProjectMediaTypes.PROJECT_ARTIFACT_CONFIG);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String schemaVersion;
        private String name;
        private String description;
        private String type;
        private String license;
        private String packageName;
        private List<String> labels = List.of();

        private Builder() {}

        public Builder schemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(String type) {
            if (StringUtils.hasText(type)) {
                this.type = type.strip();
            }
            return this;
        }

        public Builder license(String license) {
            if (StringUtils.hasText(license)) {
                this.license = license.strip();
            }
            return this;
        }

        public Builder packageName(String packageName) {
            if (StringUtils.hasText(packageName)) {
                this.packageName = packageName;
            }
            return this;
        }

        public Builder labels(List<String> labels) {
            if (!CollectionUtils.isEmpty(labels)) {
                this.labels = List.copyOf(labels);
            }
            return this;
        }

        public ProjectConfig build() {
            return new ProjectConfig(schemaVersion, name, description, type, license, packageName, labels);
        }

    }

}
