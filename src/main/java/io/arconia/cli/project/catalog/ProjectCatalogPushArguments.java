package io.arconia.cli.project.catalog;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Gathers all the information needed to push a project catalog as an OCI artifact.
 */
public record ProjectCatalogPushArguments(
    String ref,
    String tag,
    String name,
    String description,
    Map<String, String> annotations,
    @Nullable List<String> projects,
    @Nullable String fromReport,
    @Nullable String reportFileName
) {

    public ProjectCatalogPushArguments {
        Assert.hasText(ref, "ref cannot be null or empty");
        Assert.hasText(tag, "tag cannot be null or empty");
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(description, "description cannot be null or empty");
        Assert.notNull(annotations, "annotations cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String ref;
        private String tag;
        private String name;
        private String description;
        private Map<String, String> annotations;
        private @Nullable List<String> projects;
        private @Nullable String fromReport;
        private @Nullable String reportFileName;

        private Builder() {}

        public Builder ref(String ref) {
            this.ref = ref;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
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

        public Builder annotations(Map<String, String> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder fromReport(@Nullable String fromReport) {
            this.fromReport = fromReport;
            return this;
        }

        public Builder projects(@Nullable List<String> projects) {
            this.projects = projects;
            return this;
        }

        public Builder reportFileName(@Nullable String reportFileName) {
            this.reportFileName = reportFileName;
            return this;
        }

        public ProjectCatalogPushArguments build() {
            return new ProjectCatalogPushArguments(ref, tag, name, description, annotations, projects, fromReport, reportFileName);
        }

    }

}
