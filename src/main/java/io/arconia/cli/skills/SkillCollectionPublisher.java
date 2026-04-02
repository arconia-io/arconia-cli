package io.arconia.cli.skills;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import land.oras.ArtifactType;
import land.oras.ContainerRef;
import land.oras.Index;
import land.oras.Manifest;
import land.oras.ManifestDescriptor;
import land.oras.Registry;

import io.arconia.cli.utils.DateTimeUtils;
import io.arconia.cli.utils.GitUtils;

/**
 * Publishes a Skills Collection as an OCI Image Index to an OCI-compliant registry.
 * <p>
 * Implements the collection publishing workflow defined in the Agent Skills OCI Artifact
 * Specification. The collection is an OCI Image Index whose manifest entries
 * reference individual Skill Artifacts by digest, with per-descriptor annotations
 * that enable browsing the collection without pulling individual skills.
 */
public final class SkillCollectionPublisher {

    private final Registry registry;

    /**
     * Creates a new collection publisher using the given registry client.
     *
     * @param registry the ORAS registry client
     */
    public SkillCollectionPublisher(Registry registry) {
        Assert.notNull(registry, "registry must not be null");
        this.registry = registry;
    }

    /**
     * A skill reference to include in the collection, along with its metadata.
     *
     * @param name the skill name
     * @param version the skill version
     * @param ref the tag-based OCI reference (e.g., {@code ghcr.io/org/skills/pull-request:1.2.0})
     * @param digest the manifest digest
     * @param description optional skill description
     */
    public record CollectionSkillEntry(
        String name,
        String version,
        String ref,
        String digest,
        @Nullable String description
    ) {
        public CollectionSkillEntry {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.hasText(version, "version cannot be null or empty");
            Assert.hasText(ref, "ref cannot be null or empty");
            Assert.hasText(digest, "digest cannot be null or empty");
        }
    }

    /**
     * Result of a successful collection publish operation.
     *
     * @param ref the collection reference including the resolved digest
     * @param index the OCI Index that was pushed
     * @param skillCount the number of skills in the collection
     */
    public record PublishResult(
        String ref,
        String digest,
        Index index,
        int skillCount
    ) {
        public PublishResult {
            Assert.hasText(ref, "ref cannot be null or empty");
            Assert.hasText(digest, "digest cannot be null or empty");
            Assert.notNull(index, "index cannot be null");
        }
    }

    /**
     * Publishes a collection from a {@link ArtifactPublishReport} produced by a push operation.
     * <p>
     * The publish report already contains all necessary metadata (names, refs, versions,
     * digests), so no additional registry calls are needed to resolve skill manifests.
     *
     * @param report the publish report from a prior push (single or batch)
     * @param collectionRef the target OCI reference for the collection
     * @param collectionName the collection identifier
     * @param collectionVersion the version to use for the collection itself
     * @param extraAnnotations additional annotations to include, or {@code null}
     * @return the result of the publish operation
     */
    public PublishResult publishFromReport(ArtifactPublishReport report, ContainerRef collectionRef,
                                            String collectionName, String collectionVersion,
                                            Map<String, String> extraAnnotations) {
        Assert.notNull(report, "report cannot be null");
        Assert.notNull(collectionRef, "collectionRef cannot be null");
        Assert.hasText(collectionName, "collectionName cannot be null or empty");
        Assert.hasText(collectionVersion, "collectionVersion cannot be null or empty");
        Assert.notNull(extraAnnotations, "extraAnnotations cannot be null");

        List<CollectionSkillEntry> entries = report.artifacts().stream()
            .map(s -> new CollectionSkillEntry(
                s.name(),
                s.version(),
                s.ref(),
                s.digest(),
                s.description()
            ))
            .toList();

        return publish(entries, collectionRef, collectionName, collectionVersion, extraAnnotations);
    }

    /**
     * Publishes a collection from a list of explicit skill OCI references.
     * <p>
     * For each skill reference, the manifest is fetched from the registry to
     * resolve the digest and extract annotations (name, version, description).
     *
     * @param skillRefs the list of skill OCI references (e.g., {@code ghcr.io/org/skills/pull-request:1.2.0})
     * @param collectionRef the target OCI reference for the collection
     * @param collectionName the collection identifier
     * @param collectionVersion the collection version
     * @return the result of the publish operation
     */
    public PublishResult publishFromRefs(List<String> skillRefs, ContainerRef collectionRef,
                                          String collectionName, String collectionVersion,
                                          Map<String, String> extraAnnotations) {
        Assert.notNull(skillRefs, "skillRefs cannot be null");
        Assert.notNull(collectionRef, "collectionRef cannot be null");
        Assert.hasText(collectionName, "collectionName cannot be null or empty");
        Assert.hasText(collectionVersion, "collectionVersion cannot be null or empty");
        Assert.notNull(extraAnnotations, "extraAnnotations cannot be null");

        List<CollectionSkillEntry> entries = new ArrayList<>();

        for (String ref : skillRefs) {
            ContainerRef skillContainerRef = ContainerRef.parse(ref);
            Manifest manifest = registry.getManifest(skillContainerRef);

            String digest = manifest.getDigest();
            Map<String, String> annotations = manifest.getAnnotations();

            String fallbackName = SkillRef.parse(ref).skillName();
            String name = annotations != null ? annotations.getOrDefault(SkillAnnotations.SKILL_NAME, fallbackName) : fallbackName;
            String version = annotations != null ? annotations.getOrDefault(SkillAnnotations.OCI_VERSION, collectionVersion) : collectionVersion;
            String description = annotations != null ? annotations.get(SkillAnnotations.OCI_DESCRIPTION) : null;

            entries.add(new CollectionSkillEntry(name, version, ref, digest, description));
        }

        return publish(entries, collectionRef, collectionName, collectionVersion, extraAnnotations);
    }

    /**
     * Core publish logic: builds the OCI Index from skill entries and pushes it.
     */
    private PublishResult publish(List<CollectionSkillEntry> entries, ContainerRef collectionRef,
                                   String collectionName, String collectionVersion,
                                   Map<String, String> extraAnnotations) {
        // 1. Build ManifestDescriptors for each skill
        List<ManifestDescriptor> descriptors = entries.stream()
            .map(this::buildManifestDescriptor)
            .toList();

        // 2. Build the OCI Index
        // Note: OCI Image Index does not support a config descriptor per the spec.
        // Collection metadata is conveyed via annotations on the index and its manifest descriptors.
        Index index = Index.fromManifests(descriptors)
            .withArtifactType(ArtifactType.from(SkillMediaTypes.COLLECTION_ARTIFACT_TYPE))
            .withAnnotations(buildCollectionAnnotations(collectionName, collectionVersion, extraAnnotations));

        // 3. Push the index
        Index pushed = registry.pushIndex(collectionRef, index);

        // Resolve the digest from the ManifestDescriptor set by the registry response.
        // Note: Index does not override getDigest() like Manifest does, so we must
        // go through getDescriptor(). The SDK guarantees the descriptor digest is always
        // populated (from docker-content-digest header, ref digest, or computed from content).
        String digest = pushed.getDescriptor().getDigest();

        // Store the ORIGINAL pre-push index for re-tagging.
        // The post-push Index object may have extra fields (e.g. digest)
        // that alter the JSON, causing a different digest on re-push.
        return new PublishResult(
            "%s@%s".formatted(collectionRef, digest),
            digest,
            index,
            entries.size()
        );
    }

    /**
     * Tags an already-published collection index with an additional tag.
     *
     * @param previousResult the result from a prior publish call
     * @param baseRef the base collection reference without tag or digest (e.g., {@code ghcr.io/org/skills-collection})
     * @param newTag the additional tag to apply
     * @return the index as stored under the new tag
     */
    public Index tag(PublishResult previousResult, String baseRef, String newTag) {
        Assert.notNull(previousResult, "previousResult cannot be null");
        Assert.hasText(baseRef, "baseRef cannot be null or empty");
        Assert.hasText(newTag, "newTag cannot be null or empty");

        ContainerRef containerRef = ContainerRef.parse("%s:%s".formatted(baseRef, newTag));
        return registry.pushIndex(containerRef, previousResult.index());
    }

    /**
     * Builds a {@link ManifestDescriptor} for a skill entry with enriched annotations.
     */
    private ManifestDescriptor buildManifestDescriptor(CollectionSkillEntry entry) {
        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put(SkillAnnotations.SKILL_NAME, entry.name());
        annotations.put(SkillAnnotations.OCI_VERSION, entry.version());
        annotations.put(SkillAnnotations.OCI_TITLE, entry.name());
        annotations.put(SkillAnnotations.SKILL_REF, entry.ref());

        if (StringUtils.hasText(entry.description())) {
            annotations.put(SkillAnnotations.OCI_DESCRIPTION, entry.description());
        }

        // Create a descriptor referencing the skill manifest by digest.
        // Size is set to 0 as we don't track manifest sizes in the publish report;
        // the registry will resolve the actual size.
        return ManifestDescriptor.of(
                "application/vnd.oci.image.manifest.v1+json",
                entry.digest(),
                0
            )
            .withArtifactType(SkillMediaTypes.SKILL_ARTIFACT_TYPE)
            .withAnnotations(annotations);
    }

    /**
     * Builds collection-level annotations for the OCI Index.
     * <p>
     * Includes standard OCI annotations, collection-specific annotations,
     * auto-detected git metadata (source URL and revision), and any
     * user-provided extra annotations.
     * <p>
     * Precedence (highest wins): extra annotations &gt; auto-detected &gt; defaults.
     */
    private Map<String, String> buildCollectionAnnotations(String collectionName, String collectionVersion, Map<String, String> extraAnnotations) {
        Map<String, String> annotations = new LinkedHashMap<>();

        // Standard OCI annotations
        annotations.put(SkillAnnotations.OCI_CREATED, DateTimeUtils.nowIso());
        annotations.put(SkillAnnotations.OCI_TITLE, collectionName);
        annotations.put(SkillAnnotations.OCI_DESCRIPTION, "Agent Skills Collection");
        annotations.put(SkillAnnotations.OCI_VERSION, collectionVersion);

        // Auto-detect source and revision from git (best-effort, uses current directory)
        String remoteUrl = GitUtils.getRemoteUrl(null);
        if (StringUtils.hasText(remoteUrl)) {
            annotations.put(SkillAnnotations.OCI_SOURCE, remoteUrl);
        }
        String revision = GitUtils.getRevision(null);
        if (StringUtils.hasText(revision)) {
            annotations.put(SkillAnnotations.OCI_REVISION, revision);
        }

        // Collection-specific annotations
        annotations.put(SkillAnnotations.COLLECTION_NAME, collectionName);

        // User-provided annotations override everything
        if (!CollectionUtils.isEmpty(extraAnnotations)) {
            annotations.putAll(extraAnnotations);
        }

        return annotations;
    }

}
