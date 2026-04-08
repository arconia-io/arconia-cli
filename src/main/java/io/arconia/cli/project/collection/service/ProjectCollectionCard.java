package io.arconia.cli.project.collection.service;

import java.util.List;

import org.springframework.util.Assert;

/**
 * Overview of a project collection.
 */
public record ProjectCollectionCard(ProjectCollectionHeader header, List<ProjectCollectionSummary> summaries) {

    public ProjectCollectionCard {
        Assert.notNull(header, "header cannot be null");
        Assert.notNull(summaries, "summaries cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ProjectCollectionHeader header;
        private List<ProjectCollectionSummary> summaries;

        private Builder() {}

        public Builder header(ProjectCollectionHeader header) {
            this.header = header;
            return this;
        }

        public Builder summaries(List<ProjectCollectionSummary> summaries) {
            this.summaries = summaries;
            return this;
        }

        public ProjectCollectionCard build() {
            return new ProjectCollectionCard(header, summaries);
        }

    }

}
