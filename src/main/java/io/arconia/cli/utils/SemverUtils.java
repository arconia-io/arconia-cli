package io.arconia.cli.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Utility methods for working with Semantic Versioning (semver) strings.
 * <p>
 * Supports versions with an optional {@code v} prefix (e.g., {@code 1.2.3} or {@code v1.2.3})
 * and optional pre-release suffixes (e.g., {@code 1.2.3-beta.1}).
 * <p>
 * Per semver 2.0.0, pre-release versions have lower precedence than the associated
 * release version: {@code 1.0.0-alpha} &lt; {@code 1.0.0}.
 */
public final class SemverUtils {

    private SemverUtils() {}

    /**
     * Pattern matching semver versions: optional {@code v} prefix, then {@code MAJOR.MINOR.PATCH},
     * with an optional pre-release suffix.
     */
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
        "^v?(\\d+)\\.(\\d+)\\.(\\d+)(-[\\w.]+)?$"
    );

    /**
     * Returns {@code true} if the given string is a valid semver version.
     *
     * @param version the version string to check
     * @return {@code true} if the string matches the semver pattern
     */
    public static boolean isSemver(String version) {
        Assert.notNull(version, "version cannot be null");
        return SEMVER_PATTERN.matcher(version).matches();
    }

    /**
     * Finds the highest stable (non-pre-release) semver tag from a list of tags.
     * Non-semver tags (e.g., {@code "latest"}, {@code "sha-xxx"}) and pre-release
     * versions (e.g., {@code "1.0.0-beta.1"}) are ignored.
     *
     * @param tags the list of tags to search
     * @return the highest stable semver tag, or empty if none found
     */
    public static Optional<String> findHighestTag(List<String> tags) {
        Assert.notNull(tags, "tags cannot be null");
        if (tags.isEmpty()) {
            return Optional.empty();
        }
        return tags.stream()
            .filter(SemverUtils::isSemver)
            .filter(SemverUtils::isRelease)
            .max(Comparator.comparing(SemverUtils::toComparableString));
    }

    /**
     * Returns {@code true} if the given semver string is a stable release (no pre-release suffix).
     *
     * @param version a valid semver string
     * @return {@code true} if the version has no pre-release suffix
     */
    public static boolean isRelease(String version) {
        Assert.notNull(version, "version cannot be null");
        Matcher matcher = SEMVER_PATTERN.matcher(version);
        return matcher.matches() && matcher.group(4) == null;
    }

    /**
     * Compares two semver strings.
     * <p>
     * Pre-release versions sort lower than their release counterparts:
     * {@code 1.0.0-alpha} &lt; {@code 1.0.0}.
     *
     * @param a the first version
     * @param b the second version
     * @return a positive value if {@code a > b}, negative if {@code a < b}, zero if equal
     */
    public static int compare(String a, String b) {
        return toComparableString(a).compareTo(toComparableString(b));
    }

    /**
     * Converts a semver string to a zero-padded comparable representation.
     * <p>
     * Format: {@code "MAJOR.MINOR.PATCH.RELEASE_FLAG"} where each numeric
     * component is zero-padded to 6 digits and {@code RELEASE_FLAG} is
     * {@code "1"} for release versions and {@code "0"} for pre-release versions.
     * <p>
     * For example:
     * <ul>
     *   <li>{@code "1.2.3"} → {@code "000001.000002.000003.1"}</li>
     *   <li>{@code "1.2.3-beta.1"} → {@code "000001.000002.000003.0"}</li>
     * </ul>
     * Non-semver strings are mapped to {@code "000000.000000.000000.0"}.
     */
    private static String toComparableString(String version) {
        Matcher matcher = SEMVER_PATTERN.matcher(version);
        if (!matcher.matches()) {
            return "000000.000000.000000.0";
        }
        String preRelease = matcher.group(4);
        String releaseFlag = (preRelease == null || preRelease.isEmpty()) ? "1" : "0";
        return "%06d.%06d.%06d.%s".formatted(
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3)),
            releaseFlag
        );
    }

}
