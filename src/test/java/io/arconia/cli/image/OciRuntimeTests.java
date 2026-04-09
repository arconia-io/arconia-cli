package io.arconia.cli.image;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OciRuntime}.
 */
class OciRuntimeTests {

    @Test
    void dockerExecutableName() {
        assertThat(OciRuntime.DOCKER.getExecutableName()).isEqualTo("docker");
    }

    @Test
    void podmanExecutableName() {
        assertThat(OciRuntime.PODMAN.getExecutableName()).isEqualTo("podman");
    }

    @Test
    void detectReturnsARuntimeWhenAvailable() {
        OciRuntime runtime = OciRuntime.detect();
        assertThat(runtime).isNotNull();
        assertThat(runtime.getExecutableName()).isIn("docker", "podman");
    }

}
