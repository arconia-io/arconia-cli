package io.arconia.cli.openrewrite;

import java.util.ArrayList;
import java.util.List;

public record UpdateOptions(
    boolean dryRun,
    String rewriteRecipeName,
    String rewriteRecipeLibrary,
    List<String> params
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean dryRun = false;
        private String rewriteRecipeName;
        private String rewriteRecipeLibrary;
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

        public Builder params(List<String> params) {
            this.params = params;
            return this;
        }

        public UpdateOptions build() {
            return new UpdateOptions(dryRun, rewriteRecipeName, rewriteRecipeLibrary, params);
        }
    }
    
}
