package io.arconia.cli.project;

import java.nio.file.Path;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Gathers all the information needed to push a project as an OCI artifact.
 */
public record ProjectPushArguments(
    String ref,
    String tag,
    Map<String, String> annotations,
    Path projectPath,
    @Nullable String reportFileName
) {

    public ProjectPushArguments {
        Assert.hasText(ref, "ref cannot be null or empty");
        Assert.hasText(tag, "tag cannot be null or empty");
        Assert.notNull(annotations, "annotations cannot be null");
        Assert.notNull(projectPath, "projectPath cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String ref;
        private String tag;
        private Map<String, String> annotations;
        private Path projectPath;
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

        public Builder annotations(Map<String, String> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder projectPath(Path projectPath) {
            this.projectPath = projectPath;
            return this;
        }

        public Builder reportFileName(@Nullable String reportFileName) {
            this.reportFileName = reportFileName;
            return this;
        }

        public ProjectPushArguments build() {
            return new ProjectPushArguments(ref, tag, annotations, projectPath, reportFileName);
        }

    }

}
