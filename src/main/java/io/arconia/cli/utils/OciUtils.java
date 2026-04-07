package io.arconia.cli.utils;

public final class OciUtils {

    private OciUtils() {}

    /**
     * Extracts the base repository reference (everything before the tag) from an OCI
     * reference string. If the reference has no tag, returns the reference unchanged.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code "ghcr.io/org/skills-collection:1.0.0"} → {@code "ghcr.io/org/skills-collection"}</li>
     *   <li>{@code "ghcr.io/org/skills-collection"} → {@code "ghcr.io/org/skills-collection"}</li>
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

}
