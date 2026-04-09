package io.arconia.cli.build;

import java.util.List;

import org.jspecify.annotations.Nullable;

public record BuildImageArguments(
        @Nullable String imageName,
        @Nullable String builderImage,
        @Nullable String runImage,
        boolean cleanCache,
        boolean publishImage,
        @Nullable List<String> imagePlatforms
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        @Nullable private String imageName;
        @Nullable private String builderImage;
        @Nullable private String runImage;
        private boolean cleanCache = false;
        private boolean publishImage = false;
        @Nullable private List<String> imagePlatforms;

        private Builder() {}

        public Builder imageName(@Nullable String imageName) {
            this.imageName = imageName;
            return this;
        }

        public Builder builderImage(@Nullable String builderImage) {
            this.builderImage = builderImage;
            return this;
        }

        public Builder runImage(@Nullable String runImage) {
            this.runImage = runImage;
            return this;
        }

        public Builder cleanCache(boolean cleanCache) {
            this.cleanCache = cleanCache;
            return this;
        }

        public Builder publishImage(boolean publishImage) {
            this.publishImage = publishImage;
            return this;
        }

        public Builder imagePlatforms(@Nullable List<String> imagePlatforms) {
            this.imagePlatforms = imagePlatforms;
            return this;
        }

        public BuildImageArguments build() {
            return new BuildImageArguments(imageName, builderImage, runImage, cleanCache, publishImage, imagePlatforms);
        }

    }

}
