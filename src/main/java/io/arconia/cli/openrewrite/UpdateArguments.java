package io.arconia.cli.openrewrite;

public record UpdateArguments(
    boolean dryRun,
    String rewriteRecipeName,
    String rewriteRecipeLibrary
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean dryRun = false;
        private String rewriteRecipeName;
        private String rewriteRecipeLibrary;

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

        public UpdateArguments build() {
            return new UpdateArguments(dryRun, rewriteRecipeName, rewriteRecipeLibrary);
        }
    }
    
}
