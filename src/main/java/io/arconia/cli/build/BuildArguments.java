package io.arconia.cli.build;

import org.jspecify.annotations.Nullable;

public record BuildArguments(
    boolean clean,
    boolean skipTests,
    boolean nativeBuild,
    boolean offline,
    boolean testClasspath,
    @Nullable BuildImageArguments buildImageArguments
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean clean = false;
        private boolean skipTests = false;
        private boolean nativeBuild = false;
        private boolean offline = false;
        private boolean testClasspath = false;
        @Nullable private BuildImageArguments buildImageArguments;

        private Builder() {}

        public Builder clean(boolean clean) {
            this.clean = clean;
            return this;
        }

        public Builder skipTests(boolean skipTests) {
            this.skipTests = skipTests;
            return this;
        }

        public Builder nativeBuild(boolean nativeBuild) {
            this.nativeBuild = nativeBuild;
            return this;
        }

        public Builder offline(boolean offline) {
            this.offline = offline;
            return this;
        }

        public Builder testClasspath(boolean testClasspath) {
            this.testClasspath = testClasspath;
            return this;
        }

        public Builder buildImageArguments(BuildImageArguments buildImageArguments) {
            this.buildImageArguments = buildImageArguments;
            return this;
        }

        public BuildArguments build() {
            return new BuildArguments(clean, skipTests, nativeBuild, offline, testClasspath, buildImageArguments);
        }

    }

}
