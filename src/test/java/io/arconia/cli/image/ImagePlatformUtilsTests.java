package io.arconia.cli.image;

import org.junit.jupiter.api.Test;

import land.oras.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ImagePlatformUtils}.
 */
class ImagePlatformUtilsTests {

    // -- parsePlatform tests --

    @Test
    void parsePlatformLinuxAmd64() {
        Platform platform = ImagePlatformUtils.parsePlatform("linux/amd64");
        assertThat(platform.os()).isEqualTo("linux");
        assertThat(platform.architecture()).isEqualTo("amd64");
        assertThat(platform.variant()).isNull();
    }

    @Test
    void parsePlatformLinuxArm64() {
        Platform platform = ImagePlatformUtils.parsePlatform("linux/arm64");
        assertThat(platform.os()).isEqualTo("linux");
        assertThat(platform.architecture()).isEqualTo("arm64");
        assertThat(platform.variant()).isNull();
    }

    @Test
    void parsePlatformWithVariant() {
        Platform platform = ImagePlatformUtils.parsePlatform("linux/arm/v7");
        assertThat(platform.os()).isEqualTo("linux");
        assertThat(platform.architecture()).isEqualTo("arm");
        assertThat(platform.variant()).isEqualTo("v7");
    }

    @Test
    void parsePlatformWindowsAmd64() {
        Platform platform = ImagePlatformUtils.parsePlatform("windows/amd64");
        assertThat(platform.os()).isEqualTo("windows");
        assertThat(platform.architecture()).isEqualTo("amd64");
    }

    @Test
    void parsePlatformWithNullThrows() {
        assertThatThrownBy(() -> ImagePlatformUtils.parsePlatform(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsePlatformWithEmptyStringThrows() {
        assertThatThrownBy(() -> ImagePlatformUtils.parsePlatform(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsePlatformWithInvalidFormatThrows() {
        assertThatThrownBy(() -> ImagePlatformUtils.parsePlatform("linux"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid platform string");
    }

    @Test
    void parsePlatformWithTooManyPartsThrows() {
        assertThatThrownBy(() -> ImagePlatformUtils.parsePlatform("linux/arm/v7/extra"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid platform string");
    }

    // -- deriveArchSuffix tests --

    @Test
    void deriveArchSuffixAmd64() {
        assertThat(ImagePlatformUtils.deriveArchSuffix("linux/amd64")).isEqualTo("amd64");
    }

    @Test
    void deriveArchSuffixArm64() {
        assertThat(ImagePlatformUtils.deriveArchSuffix("linux/arm64")).isEqualTo("arm64");
    }

    @Test
    void deriveArchSuffixWithVariant() {
        assertThat(ImagePlatformUtils.deriveArchSuffix("linux/arm/v7")).isEqualTo("armv7");
    }

    @Test
    void deriveArchSuffixWithVariantV6() {
        assertThat(ImagePlatformUtils.deriveArchSuffix("linux/arm/v6")).isEqualTo("armv6");
    }

    @Test
    void deriveArchSuffixWithNullThrows() {
        assertThatThrownBy(() -> ImagePlatformUtils.deriveArchSuffix(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deriveArchSuffixWithInvalidFormatThrows() {
        assertThatThrownBy(() -> ImagePlatformUtils.deriveArchSuffix("linux"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -- platformSpecificImageName tests --

    @Test
    void platformSpecificImageNameWithRegistryAndTag() {
        assertThat(ImagePlatformUtils.platformSpecificImageName("ghcr.io/org/myapp:1.0", "linux/amd64"))
                .isEqualTo("ghcr.io/org/myapp:1.0-amd64");
    }

    @Test
    void platformSpecificImageNameWithArm64() {
        assertThat(ImagePlatformUtils.platformSpecificImageName("ghcr.io/org/myapp:1.0", "linux/arm64"))
                .isEqualTo("ghcr.io/org/myapp:1.0-arm64");
    }

    @Test
    void platformSpecificImageNameWithVariant() {
        assertThat(ImagePlatformUtils.platformSpecificImageName("ghcr.io/org/myapp:latest", "linux/arm/v7"))
                .isEqualTo("ghcr.io/org/myapp:latest-armv7");
    }

    @Test
    void platformSpecificImageNameSimpleImageWithTag() {
        assertThat(ImagePlatformUtils.platformSpecificImageName("myapp:1.0", "linux/amd64"))
                .isEqualTo("myapp:1.0-amd64");
    }

    @Test
    void platformSpecificImageNameWithoutTagThrows() {
        assertThatThrownBy(() -> ImagePlatformUtils.platformSpecificImageName("ghcr.io/org/myapp", "linux/amd64"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must include a tag");
    }

    @Test
    void platformSpecificImageNameWithNullImageNameThrows() {
        assertThatThrownBy(() -> ImagePlatformUtils.platformSpecificImageName(null, "linux/amd64"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void platformSpecificImageNameWithNullPlatformThrows() {
        assertThatThrownBy(() -> ImagePlatformUtils.platformSpecificImageName("myapp:1.0", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void platformSpecificImageNameWithPortInRegistry() {
        assertThat(ImagePlatformUtils.platformSpecificImageName("localhost:5000/myapp:1.0", "linux/amd64"))
                .isEqualTo("localhost:5000/myapp:1.0-amd64");
    }

}
