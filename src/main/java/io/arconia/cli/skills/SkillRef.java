package io.arconia.cli.skills;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import land.oras.ContainerRef;

/**
 * A parsed OCI reference for an agent skill artifact.
 * <p>
 * Wraps {@link ContainerRef} from the ORAS SDK to provide convenience
 * methods for working with skill artifact references of the form:
 * <pre>
 * ghcr.io/arconia-io/agent-skills/pull-request:1.2.0
 * ghcr.io/arconia-io/agent-skills/pull-request@sha256:abc123...
 * ghcr.io/arconia-io/agent-skills/pull-request:1.2.0@sha256:abc123...
 * </pre>
 *
 * @param registry the OCI registry hostname (e.g., {@code ghcr.io})
 * @param repository the repository path within the registry (e.g., {@code arconia-io/agent-skills/pull-request})
 * @param tag the mutable version tag, may be {@code null} if only a digest is specified
 * @param digest the immutable manifest digest, may be {@code null} if only a tag is specified
 */
public record SkillRef(
    String registry,
    String repository,
    @Nullable String tag,
    @Nullable String digest
) {

    /**
     * Default tag used when no tag is specified.
     */
    public static final String DEFAULT_TAG = "latest";

    public SkillRef {
        Assert.hasText(registry, "registry cannot be null or empty");
        Assert.hasText(repository, "repository cannot be null or empty");
        if (!StringUtils.hasText(tag) && !StringUtils.hasText(digest)) {
            throw new IllegalArgumentException("At least one of tag or digest must be specified");
        }
    }

    /**
     * Parses an OCI reference string into a {@link SkillRef}.
     *
     * @param reference the full OCI reference (e.g., {@code ghcr.io/org/repo/skill:1.0.0})
     * @return the parsed skill reference
     */
    public static SkillRef parse(String reference) {
        Assert.hasText(reference, "reference cannot be null or empty");
        ContainerRef containerRef = ContainerRef.parse(reference);
        return new SkillRef(
            containerRef.getRegistry(),
            containerRef.getFullRepository(),
            containerRef.getTag(),
            containerRef.getDigest()
        );
    }

    /**
     * Extracts the skill name from the repository path.
     * The skill name is the last segment of the repository path.
     *
     * @return the skill name (e.g., {@code pull-request} from {@code arconia-io/agent-skills/pull-request})
     */
    public String skillName() {
        int lastSlash = repository.lastIndexOf('/');
        return lastSlash >= 0 ? repository.substring(lastSlash + 1) : repository;
    }

    /**
     * Returns the effective tag, defaulting to {@value #DEFAULT_TAG} if no tag is set.
     *
     * @return the tag or {@code "latest"}
     */
    public String effectiveTag() {
        return StringUtils.hasText(tag) ? tag : DEFAULT_TAG;
    }

    /**
     * Returns the full OCI reference string with tag.
     *
     * @return reference in the form {@code registry/repository:tag}
     */
    public String fullTagReference() {
        return "%s/%s:%s".formatted(registry, repository, effectiveTag());
    }

    /**
     * Returns the full OCI reference string with digest, if available.
     *
     * @return reference in the form {@code registry/repository@digest}, or {@code null} if no digest
     */
    @Nullable
    public String fullDigestReference() {
        if (!StringUtils.hasText(digest)) {
            return null;
        }
        return "%s/%s@%s".formatted(registry, repository, digest);
    }

    /**
     * Returns the fully qualified reference, preferring the digest for pinning.
     * <ul>
     *   <li>tag + digest → {@code registry/repository:tag@digest}</li>
     *   <li>tag only    → {@code registry/repository:tag}</li>
     *   <li>digest only → {@code registry/repository@digest}</li>
     * </ul>
     *
     * @return the most precise OCI reference for this skill
     */
    public String fullReference() {
        if (StringUtils.hasText(digest) && StringUtils.hasText(tag)) {
            return "%s/%s:%s@%s".formatted(registry, repository, tag, digest);
        }
        if (StringUtils.hasText(digest)) {
            return "%s/%s@%s".formatted(registry, repository, digest);
        }
        return fullTagReference();
    }

    /**
     * Converts this skill reference to an ORAS {@link ContainerRef}.
     *
     * @return the ORAS container reference
     */
    public ContainerRef toContainerRef() {
        return ContainerRef.parse(fullReference());
    }

    /**
     * Creates a new builder for {@link SkillRef}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder pre-populated with the values from this reference,
     * allowing selective modification.
     *
     * @return a pre-populated builder
     */
    public Builder mutate() {
        return new Builder()
            .registry(registry)
            .repository(repository)
            .tag(tag)
            .digest(digest);
    }

    /**
     * Builder for {@link SkillRef}.
     */
    public static class Builder {

        private String registry;
        private String repository;
        private @Nullable String tag;
        private @Nullable String digest;

        private Builder() {}

        public Builder registry(String registry) {
            this.registry = registry;
            return this;
        }

        public Builder repository(String repository) {
            this.repository = repository;
            return this;
        }

        public Builder tag(@Nullable String tag) {
            this.tag = tag;
            return this;
        }

        public Builder digest(@Nullable String digest) {
            this.digest = digest;
            return this;
        }

        public SkillRef build() {
            return new SkillRef(registry, repository, tag, digest);
        }

    }

}
