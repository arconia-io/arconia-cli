package io.arconia.cli.artifact;

import org.junit.jupiter.api.Test;

import land.oras.Registry;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ArtifactPublisher}.
 */
class ArtifactPublisherTests {

    @Test
    void whenRegistryIsNullThenThrow() {
        assertThatThrownBy(() -> new ArtifactPublisher(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registry cannot be null");
    }

    @Test
    void whenArtifactInfoIsNullThenThrow() {
        ArtifactPublisher publisher = new ArtifactPublisher(Registry.builder().build());

        assertThatThrownBy(() -> publisher.publish(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifactInfo cannot be null");
    }

}
