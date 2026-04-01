package io.arconia.cli.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

public final class OciUtils {

    private OciUtils() {}

    /**
     * Extracts the base repository reference (everything before the tag) from an OCI
     * reference string. If the reference has no tag, returns the reference unchanged.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code "ghcr.io/org/skills-catalog:1.0.0"} → {@code "ghcr.io/org/skills-catalog"}</li>
     *   <li>{@code "ghcr.io/org/skills-catalog"} → {@code "ghcr.io/org/skills-catalog"}</li>
     * </ul>
     *
     * @param ref the OCI reference string
     * @return the reference without the tag portion
     */
    public static String extractRepoBase(String ref) {
        int colonIdx = ref.lastIndexOf(':');
        if (colonIdx > 0 && !ref.substring(colonIdx).contains("/")) {
            return ref.substring(0, colonIdx);
        }
        return ref;
    }

    /**
     * Parses a list of {@code key=value} strings into a map.
     */
    public static Map<String, String> parseAnnotations(List<String> pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(pairs)) {
            return map;
        }
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException(
                        "Invalid annotation format '%s': expected key=value".formatted(pair));
            }
            map.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
        return map;
    }

}
