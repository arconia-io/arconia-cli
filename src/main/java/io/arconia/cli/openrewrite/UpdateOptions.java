package io.arconia.cli.openrewrite;

import java.util.ArrayList;
import java.util.List;

public record UpdateOptions(
    boolean dryRun,
    String springBootVersion,
    String rewritePluginVersion,
    String springRecipesVersion,
    List<String> params
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean dryRun = false;
        private String springBootVersion;
        private String rewritePluginVersion;
        private String springRecipesVersion;
        private List<String> params = new ArrayList<>();

        private Builder() {}

        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public Builder springBootVersion(String springBootVersion) {
            this.springBootVersion = springBootVersion;
            return this;
        }

        public Builder rewritePluginVersion(String rewritePluginVersion) {
            this.rewritePluginVersion = rewritePluginVersion;
            return this;
        }

        public Builder springRecipesVersion(String springRecipesVersion) {
            this.springRecipesVersion = springRecipesVersion;
            return this;
        }

        public Builder params(List<String> params) {
            this.params = params;
            return this;
        }

        public UpdateOptions build() {
            return new UpdateOptions(dryRun, springBootVersion, rewritePluginVersion, springRecipesVersion, params);
        }
    }
    
}
