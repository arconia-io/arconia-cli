package io.arconia.cli.skills;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import land.oras.ContainerRef;
import land.oras.Index;
import land.oras.ManifestDescriptor;
import land.oras.Registry;

/**
 * Reads and parses a Skills Catalog from an OCI-compliant registry.
 * <p>
 * A catalog is an OCI Image Index ({@code application/vnd.oci.image.index.v1+json})
 * whose manifest descriptors carry per-skill annotations. This reader fetches the
 * index and extracts structured skill entries from those annotations, allowing
 * clients to browse the catalog without pulling individual skill artifacts.
 *
 * @see SkillCatalogPublisher
 */
public final class SkillCatalogReader {

    private final Registry registry;

    /**
     * Creates a new catalog reader using the given registry client.
     *
     * @param registry the ORAS registry client
     */
    public SkillCatalogReader(Registry registry) {
        Assert.notNull(registry, "registry cannot be null");
        this.registry = registry;
    }

    /**
     * A single skill entry extracted from a catalog index.
     *
     * @param name the skill name
     * @param version the skill version
     * @param description the skill description, or {@code null}
     * @param ref the canonical tag-based OCI reference
     * @param digest the skill manifest digest
     */
    public record CatalogSkillInfo(
        String name,
        @Nullable String version,
        @Nullable String description,
        @Nullable String ref,
        String digest
    ) {
        public CatalogSkillInfo {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.hasText(digest, "digest cannot be null or empty");
        }
    }

    /**
     * Result of reading a catalog from the registry.
     *
     * @param catalogName the catalog identifier from annotations
     * @param catalogVersion the catalog version from annotations
     * @param description the catalog description
     * @param skills the list of skill entries
     * @param index the raw OCI Index
     */
    public record CatalogInfo(
        @Nullable String catalogName,
        @Nullable String catalogVersion,
        @Nullable String description,
        List<CatalogSkillInfo> skills,
        Index index
    ) {
        public CatalogInfo {
            Assert.notNull(skills, "skills cannot be null");
            Assert.notNull(index, "index cannot be null");
        }
    }

    /**
     * Fetches and parses a catalog from the registry.
     * <p>
     * Only the catalog index manifest is fetched — individual skill artifacts
     * are NOT downloaded. All skill metadata is extracted from the descriptor
     * annotations within the index.
     *
     * @param catalogRef the OCI reference for the catalog (e.g., {@code ghcr.io/org/skills-catalog:latest})
     * @return the parsed catalog information
     */
    public CatalogInfo read(String catalogRef) {
        Assert.hasText(catalogRef, "catalogRef cannot be empty");

        ContainerRef containerRef = ContainerRef.parse(catalogRef);
        Index index = registry.getIndex(containerRef);

        // Extract catalog-level annotations
        Map<String, String> indexAnnotations = index.getAnnotations();
        String catalogName = getAnnotation(indexAnnotations, SkillAnnotations.CATALOG_NAME);
        String catalogVersion = getAnnotation(indexAnnotations, SkillAnnotations.OCI_VERSION);
        String description = getAnnotation(indexAnnotations, SkillAnnotations.OCI_DESCRIPTION);

        // Extract skill entries from manifest descriptors
        List<ManifestDescriptor> manifests = index.getManifests();
        if (manifests == null) {
            return new CatalogInfo(catalogName, catalogVersion, description, Collections.emptyList(), index);
        }

        List<CatalogSkillInfo> skills = manifests.stream()
            .map(this::extractSkillInfo)
            .toList();

        return new CatalogInfo(catalogName, catalogVersion, description, skills, index);
    }

    /**
     * Extracts a {@link CatalogSkillInfo} from a manifest descriptor's annotations.
     */
    private CatalogSkillInfo extractSkillInfo(ManifestDescriptor descriptor) {
        Map<String, String> annotations = descriptor.getAnnotations();

        String name = getAnnotation(annotations, SkillAnnotations.SKILL_NAME);
        if (!StringUtils.hasText(name)) {
            // Fall back to OCI title
            name = getAnnotation(annotations, SkillAnnotations.OCI_TITLE);
        }
        if (!StringUtils.hasText(name)) {
            // Last resort: use the digest as identifier
            name = descriptor.getDigest();
        }

        String version = getAnnotation(annotations, SkillAnnotations.OCI_VERSION);
        String desc = getAnnotation(annotations, SkillAnnotations.OCI_DESCRIPTION);
        String ref = getAnnotation(annotations, SkillAnnotations.SKILL_REF);
        String digest = descriptor.getDigest();

        return new CatalogSkillInfo(name, version, desc, ref, digest);
    }

    /**
     * Safely gets an annotation value from a map that may be {@code null}.
     */
    @Nullable
    private String getAnnotation(@Nullable Map<String, String> annotations, String key) {
        if (annotations == null) {
            return null;
        }
        return annotations.get(key);
    }

}
