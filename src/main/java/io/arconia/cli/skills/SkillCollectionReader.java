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

import io.arconia.cli.artifact.ArtifactAnnotations;

/**
 * Reads and parses a Skills Collection from an OCI-compliant registry.
 * <p>
 * A collection is an OCI Image Index ({@code application/vnd.oci.image.index.v1+json})
 * whose manifest descriptors carry per-skill annotations. This reader fetches the
 * index and extracts structured skill entries from those annotations, allowing
 * clients to browse the collection without pulling individual skill artifacts.
 *
 * @see SkillCollectionPublisher
 */
public final class SkillCollectionReader {

    private final Registry registry;

    /**
     * Creates a new collection reader using the given registry client.
     *
     * @param registry the ORAS registry client
     */
    public SkillCollectionReader(Registry registry) {
        Assert.notNull(registry, "registry cannot be null");
        this.registry = registry;
    }

    /**
     * A single skill entry extracted from a collection index.
     *
     * @param name the skill name
     * @param version the skill version
     * @param description the skill description, or {@code null}
     * @param ref the canonical tag-based OCI reference
     * @param digest the skill manifest digest
     */
    public record CollectionSkillInfo(
        String name,
        @Nullable String version,
        @Nullable String description,
        @Nullable String ref,
        String digest
    ) {
        public CollectionSkillInfo {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.hasText(digest, "digest cannot be null or empty");
        }
    }

    /**
     * Result of reading a collection from the registry.
     *
     * @param collectionName the collection identifier from annotations
     * @param collectionVersion the collection version from annotations
     * @param description the collection description
     * @param skills the list of skill entries
     * @param index the raw OCI Index
     */
    public record CollectionInfo(
        @Nullable String collectionName,
        @Nullable String collectionVersion,
        @Nullable String description,
        List<CollectionSkillInfo> skills,
        Index index
    ) {
        public CollectionInfo {
            Assert.notNull(skills, "skills cannot be null");
            Assert.notNull(index, "index cannot be null");
        }
    }

    /**
     * Fetches and parses a collection from the registry.
     * <p>
     * Only the collection index manifest is fetched — individual skill artifacts
     * are NOT downloaded. All skill metadata is extracted from the descriptor
     * annotations within the index.
     *
     * @param collectionRef the OCI reference for the collection (e.g., {@code ghcr.io/org/skills-collection:latest})
     * @return the parsed collection information
     */
    public CollectionInfo read(String collectionRef) {
        Assert.hasText(collectionRef, "collectionRef cannot be empty");

        ContainerRef containerRef = ContainerRef.parse(collectionRef);
        Index index = registry.getIndex(containerRef);

        // Extract collection-level annotations
        Map<String, String> indexAnnotations = index.getAnnotations();
        String collectionName = getAnnotation(indexAnnotations, SkillAnnotations.COLLECTION_NAME);
        String collectionVersion = getAnnotation(indexAnnotations, ArtifactAnnotations.OCI_VERSION);
        String description = getAnnotation(indexAnnotations, ArtifactAnnotations.OCI_DESCRIPTION);

        // Extract skill entries from manifest descriptors
        List<ManifestDescriptor> manifests = index.getManifests();
        if (manifests == null) {
            return new CollectionInfo(collectionName, collectionVersion, description, Collections.emptyList(), index);
        }

        List<CollectionSkillInfo> skills = manifests.stream()
            .map(this::extractSkillInfo)
            .toList();

        return new CollectionInfo(collectionName, collectionVersion, description, skills, index);
    }

    /**
     * Extracts a {@link CollectionSkillInfo} from a manifest descriptor's annotations.
     */
    private CollectionSkillInfo extractSkillInfo(ManifestDescriptor descriptor) {
        Map<String, String> annotations = descriptor.getAnnotations();

        String name = getAnnotation(annotations, SkillAnnotations.SKILL_NAME);
        if (!StringUtils.hasText(name)) {
            // Fall back to OCI title
            name = getAnnotation(annotations, ArtifactAnnotations.OCI_TITLE);
        }
        if (!StringUtils.hasText(name)) {
            // Last resort: use the digest as identifier
            name = descriptor.getDigest();
        }

        String version = getAnnotation(annotations, ArtifactAnnotations.OCI_VERSION);
        String desc = getAnnotation(annotations, ArtifactAnnotations.OCI_DESCRIPTION);
        String ref = getAnnotation(annotations, SkillAnnotations.SKILL_REF);
        String digest = descriptor.getDigest();

        return new CollectionSkillInfo(name, version, desc, ref, digest);
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
