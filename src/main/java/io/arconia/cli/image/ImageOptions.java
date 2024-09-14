package io.arconia.cli.image;

public record ImageOptions(
    String registry,
    String group,
    String name,
    String tag
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String registry;
        private String group;
        private String name;
        private String tag;

        private Builder() {}

        public Builder registry(String registry) {
            this.registry = registry;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public ImageOptions build() {
            return new ImageOptions(registry, group, name, tag);
        }
    }

}
