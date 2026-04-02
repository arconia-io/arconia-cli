package io.arconia.cli.skills;

/**
 * Media type constants for Agent Skills OCI artifacts.
 * <p>
 * All media types use the {@code application/vnd.agentskills} vendor tree
 * as defined in the <a href="https://agentskills.io/specification">Agent Skills OCI Artifact Specification</a>.
 */
public final class SkillMediaTypes {

    private SkillMediaTypes() {}

    /**
     * Artifact type for an individual skill artifact.
     */
    public static final String SKILL_ARTIFACT_TYPE = "application/vnd.agentskills.skill.v1";

    /**
     * Media type for the config object of an individual skill artifact.
     */
    public static final String SKILL_CONFIG = "application/vnd.agentskills.skill.config.v1+json";

    /**
     * Media type for the content layer of an individual skill artifact (tar+gzip).
     */
    public static final String SKILL_CONTENT_LAYER = "application/vnd.agentskills.skill.content.v1.tar+gzip";

    /**
     * Artifact type for a skills collection index.
     */
    public static final String COLLECTION_ARTIFACT_TYPE = "application/vnd.agentskills.collection.v1";

}
