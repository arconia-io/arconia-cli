package io.arconia.cli.skills;

/**
 * Annotation key constants for Agent Skills OCI artifacts.
 */
public final class SkillAnnotations {

    private SkillAnnotations() {}

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

    /**
     * Unique identifier for a collection.
     */
    public static final String COLLECTION_NAME = "io.agentskills.collection.name";

}
