package io.arconia.cli.openrewrite;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;

public record RewriteArguments(
    boolean dryRun,
    String rewriteRecipeName,
    @Nullable String rewriteRecipeLibrary,
    @Nullable String rewriteRecipeVersion,
    @Nullable Path rewriteConfigFile
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
        @Nullable
        private Path rewriteConfigFile;

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

        public Builder rewriteConfigFile(Path rewriteConfigFile) {
            this.rewriteConfigFile = rewriteConfigFile;
            return this;
        }

        public RewriteArguments build() {
            return new RewriteArguments(dryRun, rewriteRecipeName, rewriteRecipeLibrary, rewriteRecipeVersion, rewriteConfigFile);
        }
    }

}
