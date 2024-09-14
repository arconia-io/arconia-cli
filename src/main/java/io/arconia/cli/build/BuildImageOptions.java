package io.arconia.cli.build;

public record BuildImageOptions(
    String imageName,
    String builderImage,
    String runImage,
    boolean cleanCache,
    boolean publishImage
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String imageName;
        private String builderImage;
        private String runImage;
        private boolean cleanCache;
        private boolean publishImage;

        private Builder() {}
  
        public Builder imageName(String imageName) {
            this.imageName = imageName;
            return this;
        }

        public Builder builderImage(String builderImage) {
            this.builderImage = builderImage;
            return this;
        }

        public Builder runImage(String runImage) {
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

        public BuildImageOptions build() {
            return new BuildImageOptions(imageName, builderImage, runImage, cleanCache, publishImage);
        }
    }

}
