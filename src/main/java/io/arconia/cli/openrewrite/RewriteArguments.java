package io.arconia.cli.openrewrite;

import org.jspecify.annotations.Nullable;

public record RewriteArguments(
    boolean dryRun,
    String rewriteRecipeName,
    @Nullable String rewriteRecipeLibrary,
    @Nullable String rewriteRecipeVersion
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

        public RewriteArguments build() {
            return new RewriteArguments(dryRun, rewriteRecipeName, rewriteRecipeLibrary, rewriteRecipeVersion);
        }
    }
    
}
