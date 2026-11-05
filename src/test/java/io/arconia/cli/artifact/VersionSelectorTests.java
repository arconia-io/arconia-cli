package io.arconia.cli.artifact;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VersionSelector}.
 */
class VersionSelectorTests {

    private static final String LATEST = "latest";

    // --- highestVersion ---

    @Test
    void highestVersionWithEmptyListReturnsNull() {
        assertThat(VersionSelector.highestVersion(List.of())).isNull();
    }

    @Test
    void highestVersionWithSingleValidVersionReturnsIt() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0")))
                .isEqualTo("1.0.0");
    }

    @Test
    void highestVersionWhenAllTagsHaveSuffixReturnsNull() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0-M4", "2.1.0-SNAPSHOT"))).isNull();
    }

    @Test
    void highestVersionWhenSomeTagsHaveSuffixIgnoresThem() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0-M4", "2.1.0", "3.0.0-SNAPSHOT")))
                .isEqualTo("2.1.0");
    }

    @Test
    void highestVersionWhenNonSemanticVersionIgnoresIt() {
        assertThat(VersionSelector.highestVersion(List.of("abc", "2.1.0", "1.0.0")))
                .isEqualTo("2.1.0");
    }

    @Test
    void highestVersionWhenAllNonSemanticVersionsReturnsNull() {
        assertThat(VersionSelector.highestVersion(List.of("abc", "foo", "bar")))
                .isNull();
    }

    @Test
    void highestVersionWhenMalformedSemverReturnsNull() {
        assertThat(VersionSelector.highestVersion(List.of(".1.0", "1.0.", "1..0", "1.2.3.4", "-1.0")))
                .isNull();
    }

    @Test
    void highestVersionWhenLatestPresentReturnsLatest() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0", "2.1.0", LATEST)))
                .isEqualTo(LATEST);
    }

    @Test
    void highestVersionWhenOnlyLatestReturnsLatest() {
        assertThat(VersionSelector.highestVersion(List.of(LATEST)))
                .isEqualTo(LATEST);
    }

    @Test
    void highestVersionWhenLatestAmongSuffixedTagsReturnsLatest() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0-M4", LATEST, "2.1.0-SNAPSHOT")))
                .isEqualTo(LATEST);
    }

    @Test
    void highestVersionWhenDuplicateLatestTagsReturnsLatest() {
        assertThat(VersionSelector.highestVersion(List.of(LATEST, LATEST, "1.0.0")))
                .isEqualTo(LATEST);
    }

    // Standard semver comparison

    @Test
    void highestVersionReturnsHighestPatch() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0", "1.0.1", "1.0.2")))
                .isEqualTo("1.0.2");
    }

    @Test
    void highestVersionReturnsHighestMinor() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0", "1.9.0", "1.11.0")))
                .isEqualTo("1.11.0");
    }

    @Test
    void highestVersionReturnsHighestMajor() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.0", "2.0.0", "3.0.0")))
                .isEqualTo("3.0.0");
    }

    @Test
    void highestVersionHandlesUnsortedVersions() {
        assertThat(VersionSelector.highestVersion(List.of("1.0.2", "1.0.0", "1.0.1")))
                .isEqualTo("1.0.2");
        assertThat(VersionSelector.highestVersion(List.of("3.0.0", "1.0.0", "2.0.0")))
                .isEqualTo("3.0.0");
    }

    @Test
    void highestVersionWhenOnePartVersionConsidersValid() {
        assertThat(VersionSelector.highestVersion(List.of("1", "2", "3")))
                .isEqualTo("3");
    }

    @Test
    void highestVersionWhenTwoPartVersionConsidersValid() {
        assertThat(VersionSelector.highestVersion(List.of("1.0", "1.1", "2.0")))
                .isEqualTo("2.0");
    }

    @Test
    void highestVersionWhenMixedPartVersionsComparesCorrectly() {
        assertThat(VersionSelector.highestVersion(List.of("1", "1.1", "1.1.1")))
                .isEqualTo("1.1.1");
        assertThat(VersionSelector.highestVersion(List.of("1.2", "1.1.9")))
                .isEqualTo("1.2");
        assertThat(VersionSelector.highestVersion(List.of("2", "1.9.9")))
                .isEqualTo("2");
        assertThat(VersionSelector.highestVersion(List.of("2.0", "2.0.1")))
                .isEqualTo("2.0.1");
    }

    @Test
    void highestVersionWhenOneAndThreePartVersionsEquivalentReturnsFirstEquivalent() {
        assertThat(VersionSelector.highestVersion(List.of("1", "1.0.0")))
                .isEqualTo("1");
    }

}
