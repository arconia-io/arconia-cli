package io.arconia.cli.artifact;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ArtifactAnnotations}.
 */
class ArtifactAnnotationsTests {

    @Test
    void parseAnnotationsReturnsEmptyMapForNull() {
        assertThat(ArtifactAnnotations.parseAnnotations(null)).isEmpty();
    }

    @Test
    void parseAnnotationsReturnsEmptyMapForEmptyList() {
        assertThat(ArtifactAnnotations.parseAnnotations(List.of())).isEmpty();
    }

    @Test
    void parseAnnotationsParsesKeyValuePair() {
        Map<String, String> result = ArtifactAnnotations.parseAnnotations(List.of("key=value"));
        assertThat(result).containsEntry("key", "value");
    }

    @Test
    void parseAnnotationsPreservesInsertionOrder() {
        Map<String, String> result = ArtifactAnnotations.parseAnnotations(
                List.of("alpha=1", "beta=2", "gamma=3"));
        assertThat(result.keySet()).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    void parseAnnotationsHandlesMultiplePairs() {
        Map<String, String> result = ArtifactAnnotations.parseAnnotations(
                List.of("org.opencontainers.image.title=My App", "org.opencontainers.image.version=1.0.0"));
        assertThat(result).hasSize(2)
                .containsEntry("org.opencontainers.image.title", "My App")
                .containsEntry("org.opencontainers.image.version", "1.0.0");
    }

    @Test
    void parseAnnotationsHandlesValueWithEqualsSign() {
        Map<String, String> result = ArtifactAnnotations.parseAnnotations(List.of("key=val=ue"));
        assertThat(result).containsEntry("key", "val=ue");
    }

    @Test
    void parseAnnotationsHandlesEmptyValue() {
        Map<String, String> result = ArtifactAnnotations.parseAnnotations(List.of("key="));
        assertThat(result).containsEntry("key", "");
    }

    @Test
    void parseAnnotationsThrowsForMissingEqualsSign() {
        assertThatThrownBy(() -> ArtifactAnnotations.parseAnnotations(List.of("keyvalue")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid annotation format 'keyvalue'");
    }

    @Test
    void parseAnnotationsThrowsForEqualsSignAtStart() {
        assertThatThrownBy(() -> ArtifactAnnotations.parseAnnotations(List.of("=value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid annotation format '=value'");
    }

}
