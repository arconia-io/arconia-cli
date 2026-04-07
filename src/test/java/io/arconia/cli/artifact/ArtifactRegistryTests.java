package io.arconia.cli.artifact;

import org.junit.jupiter.api.Test;

import land.oras.Registry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArtifactRegistry}.
 */
class ArtifactRegistryTests {

    @Test
    void createStandardRegistry() {
        assertThat(ArtifactRegistry.create()).isNotNull().isInstanceOf(Registry.class);
        assertThat(ArtifactRegistry.create().isInsecure()).isFalse();
        assertThat(ArtifactRegistry.create().getScheme()).isEqualTo("https");
    }

}
