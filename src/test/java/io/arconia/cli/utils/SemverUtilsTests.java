package io.arconia.cli.utils;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link SemverUtils}.
 */
class SemverUtilsTests {

    // --- isSemver ---

    @Test
    void isSemverRecognizesStandardVersions() {
        assertThat(SemverUtils.isSemver("1.0.0")).isTrue();
        assertThat(SemverUtils.isSemver("0.1.0")).isTrue();
        assertThat(SemverUtils.isSemver("12.34.56")).isTrue();
    }

    @Test
    void isSemverRecognizesVPrefix() {
        assertThat(SemverUtils.isSemver("v1.0.0")).isTrue();
        assertThat(SemverUtils.isSemver("v0.1.0")).isTrue();
    }

    @Test
    void isSemverRecognizesPreReleaseSuffix() {
        assertThat(SemverUtils.isSemver("1.0.0-beta.1")).isTrue();
        assertThat(SemverUtils.isSemver("1.0.0-rc1")).isTrue();
    }

    @Test
    void isSemverRejectsNonSemver() {
        assertThat(SemverUtils.isSemver("latest")).isFalse();
        assertThat(SemverUtils.isSemver("sha-abc123")).isFalse();
        assertThat(SemverUtils.isSemver("1.0")).isFalse();
        assertThat(SemverUtils.isSemver("")).isFalse();
    }

    @Test
    void isSemverThrowsForNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> SemverUtils.isSemver(null));
    }

    // --- isRelease ---

    @Test
    void isReleaseReturnsTrueForReleaseVersions() {
        assertThat(SemverUtils.isRelease("1.0.0")).isTrue();
        assertThat(SemverUtils.isRelease("v2.3.4")).isTrue();
        assertThat(SemverUtils.isRelease("12.34.56")).isTrue();
    }

    @Test
    void isReleaseReturnsFalseForPreReleaseVersions() {
        assertThat(SemverUtils.isRelease("1.0.0-beta.1")).isFalse();
        assertThat(SemverUtils.isRelease("1.0.0-M1")).isFalse();
        assertThat(SemverUtils.isRelease("1.0.0-rc1")).isFalse();
        assertThat(SemverUtils.isRelease("2.0.0-alpha")).isFalse();
    }

    @Test
    void isReleaseReturnsFalseForNonSemver() {
        assertThat(SemverUtils.isRelease("latest")).isFalse();
        assertThat(SemverUtils.isRelease("")).isFalse();
    }

    @Test
    void isReleaseThrowsForNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> SemverUtils.isRelease(null));
    }

    // --- findHighestTag ---

    @Test
    void findHighestTagReturnsHighestVersion() {
        List<String> tags = List.of("1.0.0", "2.0.1", "1.5.3", "latest");
        Optional<String> result = SemverUtils.findHighestTag(tags);
        assertThat(result).hasValue("2.0.1");
    }

    @Test
    void findHighestTagIgnoresNonSemverTags() {
        List<String> tags = List.of("latest", "sha-abc", "nightly");
        Optional<String> result = SemverUtils.findHighestTag(tags);
        assertThat(result).isEmpty();
    }

    @Test
    void findHighestTagHandlesSingleTag() {
        Optional<String> result = SemverUtils.findHighestTag(List.of("3.0.0"));
        assertThat(result).hasValue("3.0.0");
    }

    @Test
    void findHighestTagHandlesVPrefixedTags() {
        List<String> tags = List.of("v1.0.0", "v2.0.0", "latest");
        Optional<String> result = SemverUtils.findHighestTag(tags);
        assertThat(result).hasValue("v2.0.0");
    }

    @Test
    void findHighestTagReturnsEmptyForEmptyList() {
        assertThat(SemverUtils.findHighestTag(List.of())).isEmpty();
    }

    @Test
    void findHighestTagThrowsForNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> SemverUtils.findHighestTag(null));
    }

    // --- compare ---

    @Test
    void compareReturnsPositiveWhenFirstIsGreater() {
        assertThat(SemverUtils.compare("2.0.0", "1.0.0")).isGreaterThan(0);
        assertThat(SemverUtils.compare("1.1.0", "1.0.0")).isGreaterThan(0);
        assertThat(SemverUtils.compare("1.0.1", "1.0.0")).isGreaterThan(0);
    }

    @Test
    void compareReturnsNegativeWhenFirstIsSmaller() {
        assertThat(SemverUtils.compare("1.0.0", "2.0.0")).isLessThan(0);
    }

    @Test
    void compareReturnsZeroForEqualVersions() {
        assertThat(SemverUtils.compare("1.2.3", "1.2.3")).isZero();
    }

    @Test
    void compareHandlesVPrefix() {
        assertThat(SemverUtils.compare("v2.0.0", "1.0.0")).isGreaterThan(0);
    }

    @Test
    void comparePreReleaseSortsLowerThanRelease() {
        assertThat(SemverUtils.compare("1.0.0-beta.1", "1.0.0")).isLessThan(0);
        assertThat(SemverUtils.compare("1.0.0", "1.0.0-beta.1")).isGreaterThan(0);
        assertThat(SemverUtils.compare("1.0.0-alpha", "1.0.0")).isLessThan(0);
        assertThat(SemverUtils.compare("1.0.0-rc1", "1.0.0")).isLessThan(0);
    }

    @Test
    void comparePreReleaseDoesNotAffectDifferentVersions() {
        // 2.0.0-beta is still greater than 1.0.0 (major version wins)
        assertThat(SemverUtils.compare("2.0.0-beta.1", "1.0.0")).isGreaterThan(0);
    }

    @Test
    void findHighestTagIgnoresPreReleaseTags() {
        List<String> tags = List.of("1.0.1", "1.0.2", "2.0.0-M1");
        Optional<String> result = SemverUtils.findHighestTag(tags);
        assertThat(result).hasValue("1.0.2");
    }

    @Test
    void findHighestTagReturnsEmptyWhenOnlyPreReleaseTags() {
        List<String> tags = List.of("1.0.0-beta.1", "2.0.0-M1", "3.0.0-rc1");
        Optional<String> result = SemverUtils.findHighestTag(tags);
        assertThat(result).isEmpty();
    }

}
