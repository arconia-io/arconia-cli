package io.arconia.cli.skills;

/**
 * Annotation key constants for Agent Skills OCI artifacts.
 * <p>
 * Includes both standard OCI annotations ({@code org.opencontainers.image.*})
 * and Agent Skills-specific annotations ({@code io.agentskills.*})
 * as defined in the Agent Skills OCI Artifact Specification.
 */
public final class SkillAnnotations {

    private SkillAnnotations() {}

    // --- Standard OCI annotations (org.opencontainers.image.*) ---

    /**
     * ISO 8601 timestamp of artifact creation.
     */
    public static final String OCI_CREATED = "org.opencontainers.image.created";

    /**
     * Human-readable title (skill name).
     */
    public static final String OCI_TITLE = "org.opencontainers.image.title";

    /**
     * Short description of the skill.
     */
    public static final String OCI_DESCRIPTION = "org.opencontainers.image.description";

    /**
     * URL of the source repository.
     */
    public static final String OCI_SOURCE = "org.opencontainers.image.source";

    /**
     * Source control revision (e.g., git commit SHA).
     */
    public static final String OCI_REVISION = "org.opencontainers.image.revision";

    /**
     * SPDX license expression.
     */
    public static final String OCI_LICENSES = "org.opencontainers.image.licenses";

    /**
     * Version of the packaged artifact.
     */
    public static final String OCI_VERSION = "org.opencontainers.image.version";


    // --- Agent Skills annotations (io.agentskills.*) ---

    /**
     * Skill name. Must match the {@code name} field in config and SKILL.md frontmatter.
     */
    public static final String SKILL_NAME = "io.agentskills.skill.name";

    /**
     * Compatibility notes from SKILL.md frontmatter.
     */
    public static final String SKILL_COMPATIBILITY = "io.agentskills.skill.compatibility";

    /**
     * Canonical tag-based reference for a skill within a collection.
     */
    public static final String SKILL_REF = "io.agentskills.skill.ref";

    // --- Collection annotations (io.agentskills.collection.*) ---

    /**
     * Unique identifier for a collection.
     */
    public static final String COLLECTION_NAME = "io.agentskills.collection.name";


}
