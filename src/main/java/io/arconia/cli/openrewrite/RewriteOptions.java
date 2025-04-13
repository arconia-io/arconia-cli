package io.arconia.cli.openrewrite;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;

public record RewriteOptions(
    boolean dryRun,
    String rewriteRecipeName,
    @Nullable String rewriteRecipeLibrary,
    @Nullable String rewriteRecipeVersion,
    List<String> params
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean dryRun = false;
        private String rewriteRecipeName;
        @Nullable
        private String rewriteRecipeLibrary;
        @Nullable
        private String rewriteRecipeVersion;
        private List<String> params = new ArrayList<>();

        private Builder() {}

        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public Builder rewriteRecipeName(String rewriteRecipeName) {
            this.rewriteRecipeName = rewriteRecipeName;
            return this;
        }

        public Builder rewriteRecipeLibrary(String rewriteRecipeLibrary) {
            this.rewriteRecipeLibrary = rewriteRecipeLibrary;
            return this;
        }

        public Builder rewriteRecipeVersion(String rewriteRecipeVersion) {
            this.rewriteRecipeVersion = rewriteRecipeVersion;
            return this;
        }

        public Builder params(List<String> params) {
            this.params = params;
            return this;
        }

        public RewriteOptions build() {
            return new RewriteOptions(dryRun, rewriteRecipeName, rewriteRecipeLibrary, rewriteRecipeVersion, params);
        }
    }
    
}
