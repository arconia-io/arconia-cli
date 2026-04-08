package io.arconia.cli.artifact;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VersionSelector}.
 */
class VersionSelectorTests {

    @Test
    void whenEmptyTagListThenReturnNull() {
        assertThat(VersionSelector.highestVersion(List.of())).isNull();
    }

    @Test
    void whenAllTagsHaveSuffixThenReturnNull() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0-M4", "2.1.0-SNAPSHOT"))).isNull();
    }

    @Test
    void whenSomeTagsHaveSuffixThenIgnoreThem() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0-M4", "2.1.0", "3.0.0-SNAPSHOT")))
                .isEqualTo("2.1.0");
    }

    @Test
    void whenNonSemanticVersionThenIgnoreIt() {
        assertThat(VersionSelector.highestVersion(List.of("abc", "2.1.0", "1.0.0")))
                .isEqualTo("2.1.0");
    }

    @Test
    void whenAllNonSemanticVersionsThenReturnNull() {
        assertThat(VersionSelector.highestVersion(List.of("abc", "foo", "bar")))
                .isNull();
    }

    @Test
    void whenLatestPresentThenReturnLatest() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0", "2.1.0", "latest")))
                .isEqualTo("latest");
    }

    @Test
    void whenOnlyLatestThenReturnLatest() {
        assertThat(VersionSelector.highestVersion(List.of("latest")))
                .isEqualTo("latest");
    }

    @Test
    void whenLatestAmongSuffixedTagsThenReturnLatest() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0-M4", "latest", "2.1.0-SNAPSHOT")))
                .isEqualTo("latest");
    }

    @Test
    void whenDuplicateLatestTagsThenReturnLatest() {
        assertThat(VersionSelector.highestVersion(List.of("latest", "latest", "1.0.0")))
                .isEqualTo("latest");
    }

    // Standard 3-part semver comparison

    @Test
    void whenMultipleValidVersionsThenReturnHighestPatch() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0", "1.0.1", "1.0.2")))
                .isEqualTo("1.0.2");
    }

    @Test
    void whenMultipleValidVersionsThenReturnHighestMinor() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0", "1.9.0", "1.11.0")))
                .isEqualTo("1.11.0");
    }

    @Test
    void whenMultipleValidVersionsThenReturnHighestMajor() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0", "2.0.0", "3.0.0")))
                .isEqualTo("3.0.0");
    }

    @Test
    void whenSingleValidVersionThenReturnIt() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0-M4", "2.1.0", "3.0.0-SNAPSHOT")))
                .isEqualTo("2.1.0");
    }

    @Test
    void whenOnePartVersionThenConsiderValid() {
        assertThat(VersionSelector.highestVersion(List.of("1", "2", "3")))
                .isEqualTo("3");
    }

    @Test
    void whenTwoPartVersionThenConsiderValid() {
        assertThat(VersionSelector.highestVersion(List.of("1.0", "1.1", "2.0")))
                .isEqualTo("2.0");
    }

    @Test
    void whenMixedPartVersionsThenCompareCorrectly() {
        assertThat(VersionSelector.highestVersion(List.of("1", "1.1", "1.1.1")))
                .isEqualTo("1.1.1");
    }

    @Test
    void whenOneAndThreePartVersionsEquivalentThenReturnOne() {
        assertThat(VersionSelector.highestVersion(List.of("1", "1.0.0")))
                .isEqualTo("1");
    }

}
