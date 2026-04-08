package io.arconia.cli.artifact;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * Utility class for selecting the highest version of a list of tags.
 */
public final class VersionSelector {

    private static final Pattern SEMVER_PATTERN = Pattern.compile("^\\d+(\\.\\d+){0,2}$");

    private static boolean isValid(String tag) {
        if (tag.equals("latest")) {
            return true;
        }
        if (tag.contains("-")) {
            return false;
        }
        return SEMVER_PATTERN.matcher(tag).matches();
    }

    private static int compareVersions(String a, String b) {
        if (a.equals("latest") && b.equals("latest")) {
            return 0;
        }
        if (a.equals("latest")) {
            return 1;
        }
        if (b.equals("latest")) {
            return -1;
        }

        int[] segA = Arrays.stream(a.split("\\.")).mapToInt(Integer::parseInt).toArray();
        int[] segB = Arrays.stream(b.split("\\.")).mapToInt(Integer::parseInt).toArray();

        int len = Math.max(segA.length, segB.length);

        for (int i = 0; i < len; i++) {
            int numA = i < segA.length ? segA[i] : 0;
            int numB = i < segB.length ? segB[i] : 0;
            if (numA != numB) {
                return Integer.compare(numA, numB);
            }
        }

        return 0;
    }

    @Nullable
    public static String highestVersion(List<String> tags) {
        return tags.stream()
                .filter(VersionSelector::isValid)
                .max(VersionSelector::compareVersions)
                .orElse(null);
    }

}
