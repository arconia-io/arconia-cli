package io.arconia.cli.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OciUtils}.
 */
class OciUtilsTests {

    // --- extractRepoBase ---

    @Test
    void extractRepoBaseWithTagReturnsBaseWithoutTag() {
        assertThat(OciUtils.extractRepoBase("ghcr.io/org/skills-collection:1.0.0"))
                .isEqualTo("ghcr.io/org/skills-collection");
        assertThat(OciUtils.extractRepoBase("docker.io/library/alpine:3.19"))
                .isEqualTo("docker.io/library/alpine");
        assertThat(OciUtils.extractRepoBase("ubuntu:latest"))
                .isEqualTo("ubuntu");
    }

    @Test
    void extractRepoBaseWithoutTagReturnsSameReference() {
        assertThat(OciUtils.extractRepoBase("ghcr.io/org/skills-collection"))
                .isEqualTo("ghcr.io/org/skills-collection");
        assertThat(OciUtils.extractRepoBase("docker.io/library/alpine"))
                .isEqualTo("docker.io/library/alpine");
        assertThat(OciUtils.extractRepoBase("ubuntu"))
                .isEqualTo("ubuntu");
    }

    @Test
    void extractRepoBaseWithRegistryPortAndTagReturnsBaseWithPort() {
        assertThat(OciUtils.extractRepoBase("localhost:5000/my-org/my-app:1.0.0"))
                .isEqualTo("localhost:5000/my-org/my-app");
        assertThat(OciUtils.extractRepoBase("registry.local:8080/image:v2"))
                .isEqualTo("registry.local:8080/image");
    }

    @Test
    void extractRepoBaseWithRegistryPortAndNoTagReturnsSameReference() {
        assertThat(OciUtils.extractRepoBase("localhost:5000/my-org/my-app"))
                .isEqualTo("localhost:5000/my-org/my-app");
        assertThat(OciUtils.extractRepoBase("registry.local:8080/image"))
                .isEqualTo("registry.local:8080/image");
    }

    @Test
    void extractRepoBaseWithEmptyStringReturnsEmptyString() {
        assertThat(OciUtils.extractRepoBase("")).isEmpty();
    }

}
