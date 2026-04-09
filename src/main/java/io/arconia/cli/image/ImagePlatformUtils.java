package io.arconia.cli.image;

import org.springframework.util.Assert;

import land.oras.Platform;

/**
 * Utilities for parsing OCI platform strings and deriving image tag suffixes.
 */
public final class ImagePlatformUtils {

    private ImagePlatformUtils() {}

    /**
     * Parse a platform string like "linux/amd64" or "linux/arm/v7" into an ORAS {@link Platform}.
     */
    public static Platform parsePlatform(String platformString) {
        Assert.hasText(platformString, "platformString cannot be null or empty");

        String[] parts = platformString.split("/");
        return switch (parts.length) {
            case 2 -> Platform.of(parts[0], parts[1]);
            case 3 -> Platform.of(parts[0], parts[1], parts[2]);
            default -> throw new IllegalArgumentException(
                    "Invalid platform string '%s'. Expected format: os/architecture or os/architecture/variant".formatted(platformString));
        };
    }

    /**
     * Derive a tag suffix from a platform string.
     * <p>For example:
     * <ul>
     *   <li>"linux/amd64" → "amd64"</li>
     *   <li>"linux/arm64" → "arm64"</li>
     *   <li>"linux/arm/v7" → "armv7"</li>
     * </ul>
     */
    public static String deriveArchSuffix(String platformString) {
        Assert.hasText(platformString, "platformString cannot be null or empty");

        String[] parts = platformString.split("/");
        return switch (parts.length) {
            case 2 -> parts[1];
            case 3 -> parts[1] + parts[2];
            default -> throw new IllegalArgumentException(
                    "Invalid platform string '%s'. Expected format: os/architecture or os/architecture/variant".formatted(platformString));
        };
    }

    /**
     * Compute a platform-specific image name by appending the architecture suffix to the tag.
     * <p>
     * For example, given imageName "myregistry/myapp:1.0" and platform "linux/amd64",
     * returns "myregistry/myapp:1.0-amd64".
     */
    public static String platformSpecificImageName(String imageName, String platformString) {
        Assert.hasText(imageName, "imageName cannot be null or empty");
        Assert.hasText(platformString, "platformString cannot be null or empty");

        String suffix = deriveArchSuffix(platformString);

        // Handle image names with or without registry/path prefix
        // e.g. "registry.io/org/app:tag" or "app:tag"
        int tagSeparator = imageName.lastIndexOf(':');
        if (tagSeparator < 0 || tagSeparator < imageName.lastIndexOf('/')) {
            throw new IllegalArgumentException(
                    "Image name '%s' must include a tag (e.g. myregistry/myapp:1.0) for multi-arch builds".formatted(imageName));
        }

        return "%s-%s".formatted(imageName, suffix);
    }

}
