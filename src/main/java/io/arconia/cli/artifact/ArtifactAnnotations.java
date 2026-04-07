package io.arconia.cli.artifact;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Utilities for working with OCI artifact annotations.
 * <p>
 * See <a href="https://specs.opencontainers.org/image-spec/annotations/">OCI Image Annotations</a>.
 */
public final class ArtifactAnnotations {

    private ArtifactAnnotations() {}

    /**
     * Date and time on which the image was built, conforming to RFC 3339.
     */
    public static final String OCI_CREATED = "org.opencontainers.image.created";

    /**
     * Contact details of the people or organization responsible for the image.
     */
    public static final String OCI_AUTHORS = "org.opencontainers.image.authors";

    /**
     * URL to find more information on the image.
     */
    public static final String OCI_URL = "org.opencontainers.image.url";

    /**
     * URL to get documentation on the image.
     */
    public static final String OCI_DOCUMENTATION = "org.opencontainers.image.documentation";

    /**
     * URL to get source code for building the image.
     */
    public static final String OCI_SOURCE = "org.opencontainers.image.source";

    /**
     * Version of the packaged software.
     */
    public static final String OCI_VERSION = "org.opencontainers.image.version";

    /**
     * Source control revision identifier for the packaged software.
     */
    public static final String OCI_REVISION = "org.opencontainers.image.revision";

    /**
     * Name of the distributing entity, organization, or individual.
     */
    public static final String OCI_VENDOR = "org.opencontainers.image.vendor";

    /**
     * License(s) under which contained software is distributed as an SPDX License Expression.
     */
    public static final String OCI_LICENSES = "org.opencontainers.image.licenses";

    /**
     * Name of the reference for a target.
     */
    public static final String OCI_REF_NAME = "org.opencontainers.image.ref.name";

    /**
     * Human-readable title of the image.
     */
    public static final String OCI_TITLE = "org.opencontainers.image.title";

    /**
     * Human-readable description of the software packaged in the image.
     */
    public static final String OCI_DESCRIPTION = "org.opencontainers.image.description";

    /**
     * Digest of the image this image is based on.
     */
    public static final String OCI_BASE_DIGEST = "org.opencontainers.image.base.digest";

    /**
     * Image reference of the image this image is based on.
     */
    public static final String OCI_BASE_NAME = "org.opencontainers.image.base.name";

    /**
     * Parse a list of annotation pairs into a map.
     */
    public static Map<String, String> parseAnnotations(@Nullable List<String> annotationPairs) {
        if (CollectionUtils.isEmpty(annotationPairs)) {
            return Map.of();
        }

        Map<String, String> map = new LinkedHashMap<>();
        for (String annotationPair : annotationPairs) {
            int eq = annotationPair.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException(
                        "Invalid annotation format '%s': expected key=value".formatted(annotationPair));
            }
            map.put(annotationPair.substring(0, eq), annotationPair.substring(eq + 1));
        }
        return map;
    }

}
