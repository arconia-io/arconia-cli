package io.arconia.cli.project.catalog.service;

import java.util.List;

import org.springframework.util.Assert;

/**
 * Overview of a project catalog.
 */
public record ProjectCatalogCard(ProjectCatalogHeader header, List<ProjectCatalogSummary> summaries) {

    public ProjectCatalogCard {
        Assert.notNull(header, "header cannot be null");
        Assert.notNull(summaries, "summaries cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ProjectCatalogHeader header;
        private List<ProjectCatalogSummary> summaries;

        private Builder() {}

        public Builder header(ProjectCatalogHeader header) {
            this.header = header;
            return this;
        }

        public Builder summaries(List<ProjectCatalogSummary> summaries) {
            this.summaries = summaries;
            return this;
        }

        public ProjectCatalogCard build() {
            return new ProjectCatalogCard(header, summaries);
        }

    }

}
