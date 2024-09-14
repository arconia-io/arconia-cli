package io.arconia.cli.build;

import java.util.ArrayList;
import java.util.List;

public record BuildOptions(
    boolean clean,
    boolean nativeBuild,
    boolean skipTests,
    BuildImageOptions buildImageOptions,
    List<String> params
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean clean = false;
        private boolean nativeBuild = false;
        private boolean skipTests = false;
        private BuildImageOptions buildImageOptions;
        private List<String> params = new ArrayList<>();
        
        private Builder() {}

        public Builder clean(boolean clean) {
            this.clean = clean;
            return this;
        }

        public Builder nativeBuild(boolean nativeBuild) {
            this.nativeBuild = nativeBuild;
            return this;
        }

        public Builder skipTests(boolean skipTests) {
            this.skipTests = skipTests;
            return this;
        }

        public Builder buildImageOptions(BuildImageOptions buildImageOptions) {
            this.buildImageOptions = buildImageOptions;
            return this;
        }

        public Builder params(List<String> params) {
            this.params = params;
            return this;
        }
    
        public BuildOptions build() {
            return new BuildOptions(clean, nativeBuild, skipTests, buildImageOptions, params);
        }

    }

}