package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import land.oras.Annotations;
import land.oras.ArtifactType;
import land.oras.Config;
import land.oras.ContainerRef;
import land.oras.LocalPath;
import land.oras.Manifest;
import land.oras.Registry;
import land.oras.utils.ArchiveUtils;
import land.oras.utils.SupportedAlgorithm;
import land.oras.utils.SupportedCompression;

import io.arconia.cli.utils.DateTimeUtils;
import io.arconia.cli.utils.GitUtils;
import io.arconia.cli.utils.JsonUtils;
import io.arconia.cli.utils.SemverUtils;

/**
 * Publishes agent skills as OCI artifacts to an OCI-compliant registry.
 * <p>
 * Implements the publishing workflow defined in the Agent Skills OCI Artifact Specification:
 * <ol>
 *   <li>Validates the skill directory structure (SKILL.md must exist)</li>
 *   <li>Parses SKILL.md frontmatter into a {@link SkillConfig}</li>
 *   <li>Serializes the config as the OCI config blob</li>
 *   <li>Creates a compressed tar archive of the skill directory</li>
 *   <li>Pushes the artifact with correct media types, artifact type, and annotations.</li>
 * </ol>
 */
public final class SkillPublisher {

    private final Registry registry;

    /**
     * Creates a new publisher using the given registry client.
     *
     * @param registry the ORAS registry client
     */
    public SkillPublisher(Registry registry) {
        Assert.notNull(registry, "registry cannot be null");
        this.registry = registry;
    }

    /**
     * Result of a successful skill publish operation.
     *
     * @param ref the skill reference including the resolved digest
     * @param config the skill config that was published
     * @param manifest the OCI manifest that was pushed
     */
    public record PublishResult(
        SkillRef ref,
        SkillConfig config,
        Manifest manifest
    ) {
        public PublishResult {
            Assert.notNull(ref, "ref cannot be null");
            Assert.notNull(config, "config cannot be null");
            Assert.notNull(manifest, "manifest cannot be null");
        }
    }

    /**
     * Publishes a skill directory as an OCI artifact.
     * <p>
     * Pre-conditions:
     * <ul>
     *   <li>The skill directory name MUST match the skill name declared in
     *       {@code SKILL.md} (e.g. a skill named {@code pull-request} must live in a
     *       directory also called {@code pull-request}).</li>
     *   <li>The target reference MUST carry a valid
     *       <a href="https://semver.org/">Semantic Versioning 2.0.0</a> tag (e.g.
     *       {@code 1.2.0}, {@code v1.2.0-beta.1}). Non-semver tags such as
     *       {@code latest} or floating major/minor tags ({@code 1}, {@code 1.2})
     *       should be applied afterwards via {@link #tag(PublishResult, String)}.</li>
     * </ul>
     *
     * @param skillDirectory the path to the skill directory (must contain SKILL.md, and its
     *                       name must match the skill name declared in the frontmatter)
     * @param targetRef the target OCI reference whose tag MUST be a valid semver version
     * @param extraAnnotations additional annotations to include (may override auto-detected values)
     * @return the result of the publish operation including the digest
     * @throws IOException if the skill directory cannot be read or archived
     * @throws IllegalArgumentException if the skill directory is invalid, the directory name
     *                                  does not match the skill name, or the tag is not valid semver
     */
    public PublishResult publish(Path skillDirectory, SkillRef targetRef, Map<String, String> extraAnnotations) throws IOException {
        Assert.notNull(skillDirectory, "skillDirectory cannot be null");
        Assert.notNull(targetRef, "targetRef cannot be null");
        Assert.notNull(extraAnnotations, "extraAnnotations cannot be null");

        // 1. Validate skill directory structure
        validateSkillDirectory(skillDirectory);

        // 2. Validate the tag is a proper semver version
        String version = targetRef.tag();
        if (version == null || !SemverUtils.isSemver(version)) {
            throw new IllegalArgumentException(
                "Tag '%s' is not a valid semver version. Publish requires a semver tag (e.g. 1.2.0). Use the tag() method to add non-semver tags like 'latest' afterwards."
                    .formatted(version));
        }

        // 3. Parse SKILL.md frontmatter
        SkillFrontmatter skillFrontmatter = SkillFrontmatterParser.parseFromDirectory(skillDirectory);

        // 4. Verify the directory name matches the skill name
        String skillName = skillFrontmatter.name();
        String directoryName = skillDirectory.getFileName().toString();
        if (!directoryName.equals(skillName)) {
            throw new IllegalArgumentException(
                "Skill directory name '%s' does not match skill name '%s' declared in SKILL.md. Rename the directory to match."
                    .formatted(directoryName, skillName));
        }

        // 5. Build the config blob. Version is always the semver tag.
        SkillConfig skillConfig = SkillConfig.fromFrontmatter(skillFrontmatter, version);
        Config config = buildConfig(skillConfig);

        // 6. Build manifest annotations
        Annotations annotations = Annotations.ofManifest(
                buildAnnotations(skillConfig, skillDirectory, extraAnnotations));

        // 7. Create the content layer (tar.gz of the skill directory).
        LocalPath dirPath = LocalPath.of(skillDirectory, SkillMediaTypes.SKILL_CONTENT_LAYER);
        LocalPath contentLayer = ArchiveUtils.tarcompress(dirPath, SupportedCompression.GZIP.getMediaType());
        contentLayer = LocalPath.of(contentLayer.getPath(), SkillMediaTypes.SKILL_CONTENT_LAYER);

        // 8. Push the artifact
        ContainerRef containerRef = targetRef.toContainerRef();
        ArtifactType artifactType = ArtifactType.from(SkillMediaTypes.SKILL_ARTIFACT_TYPE);
        Manifest manifest = registry.pushArtifact(containerRef, artifactType, annotations, config, contentLayer);

        // 9. Resolve the digest from the pushed manifest
        SkillRef resolvedRef = targetRef.mutate().digest(manifest.getDigest()).build();

        return new PublishResult(resolvedRef, skillConfig, manifest);
    }

    /**
     * Tags an already-published manifest with an additional tag.
     * <p>
     * This pushes the exact same manifest under a new tag in the same
     * repository, without rebuilding or re-uploading any blobs. The
     * registry will deduplicate the content layers automatically.
     *
     * @param previousResult the result from a prior {@link #publish} call
     * @param newTag the additional tag to apply (e.g., {@code "latest"}, {@code "1"})
     * @return the manifest as stored under the new tag
     */
    public Manifest tag(PublishResult previousResult, String newTag) {
        Assert.notNull(previousResult, "previousResult cannot be null");
        Assert.hasText(newTag, "newTag cannot be null or empty");

        // Build a tag-only reference (no digest) so the registry creates
        // a tag entry for the same manifest. No new digest is computed.
        SkillRef baseRef = previousResult.ref();
        String tagOnlyRef = "%s/%s:%s".formatted(baseRef.registry(), baseRef.repository(), newTag);
        ContainerRef containerRef = ContainerRef.parse(tagOnlyRef);

        return registry.pushManifest(containerRef, previousResult.manifest());
    }

    /**
     * Validates that the given path is a valid skill directory.
     *
     * @param skillDirectory the path to validate
     * @throws IllegalArgumentException if the directory is invalid
     */
    private void validateSkillDirectory(Path skillDirectory) {
        if (!Files.isDirectory(skillDirectory)) {
            throw new IllegalArgumentException(
                "Skill path is not a directory: %s".formatted(skillDirectory));
        }

        Path skillMd = skillDirectory.resolve(SkillFrontmatterParser.SKILL_FILENAME);
        if (!Files.exists(skillMd)) {
            throw new IllegalArgumentException(
                "SKILL.md not found in directory: %s".formatted(skillDirectory));
        }
    }

    /**
     * Builds the OCI config descriptor with inline skill metadata.
     * <p>
     * The skill config JSON is embedded as Base64-encoded inline data in the
     * config descriptor. This makes the structured metadata directly accessible
     * from the manifest without unpacking the content layer.
     *
     * @param skillConfig the skill config to embed
     * @return the OCI config descriptor
     */
    private Config buildConfig(SkillConfig skillConfig) {
        byte[] configBytes = JsonUtils.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(skillConfig);
        String digest = SupportedAlgorithm.SHA256.digest(configBytes);
        long size = configBytes.length;
        String dataBase64 = Base64.getEncoder().encodeToString(configBytes);

        // Construct the config descriptor JSON with inline data
        String configDescriptor = """
            {"mediaType":"%s","digest":"%s","size":%d,"data":"%s"}""".formatted(
            SkillMediaTypes.SKILL_CONFIG, digest, size, dataBase64);

        return Config.fromJson(configDescriptor);
    }

    /**
     * Builds the OCI manifest annotations from the skill config, auto-detected
     * git metadata, and any user-provided extra annotations.
     * <p>
     * Precedence (highest wins): extra annotations &gt; auto-detected &gt; config-derived.
     */
    private Map<String, String> buildAnnotations(SkillConfig config, Path skillDirectory, Map<String, String> extraAnnotations) {
        Map<String, String> annotations = new LinkedHashMap<>();

        // Standard OCI annotations
        annotations.put(SkillAnnotations.OCI_CREATED, DateTimeUtils.nowIso());
        annotations.put(SkillAnnotations.OCI_TITLE, config.name());

        if (config.description() != null) {
            annotations.put(SkillAnnotations.OCI_DESCRIPTION, config.description());
        }
        if (config.license() != null) {
            annotations.put(SkillAnnotations.OCI_LICENSES, config.license());
        }
        if (config.version() != null) {
            annotations.put(SkillAnnotations.OCI_VERSION, config.version());
        }

        // Auto-detect source and revision from Git (best-effort)
        String remoteUrl = GitUtils.getRemoteUrl(skillDirectory);
        if (StringUtils.hasText(remoteUrl)) {
            annotations.put(SkillAnnotations.OCI_SOURCE, remoteUrl);
        }
        String revision = GitUtils.getRevision(skillDirectory);
        if (StringUtils.hasText(revision)) {
            annotations.put(SkillAnnotations.OCI_REVISION, revision);
        }

        // Agent Skills annotations
        annotations.put(SkillAnnotations.SKILL_NAME, config.name());

        if (config.compatibility() != null) {
            annotations.put(SkillAnnotations.SKILL_COMPATIBILITY, config.compatibility());
        }

        // User-provided annotations override everything
        if (!CollectionUtils.isEmpty(extraAnnotations)) {
            annotations.putAll(extraAnnotations);
        }

        return annotations;
    }

}
