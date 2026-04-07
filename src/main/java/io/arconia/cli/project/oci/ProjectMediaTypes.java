package io.arconia.cli.project.oci;

/**
 * Media type constants for Projects OCI artifacts.
 */
public final class ProjectMediaTypes {

    private ProjectMediaTypes() {}

    public static final String PROJECT_ARTIFACT_TYPE = "application/vnd.arconia.project.v1";

    public static final String PROJECT_ARTIFACT_CONFIG = "application/vnd.arconia.project.config.v1+json";

    public static final String PROJECT_ARTIFACT_CONTENT_LAYER = "application/vnd.arconia.project.content.v1.tar+gzip";

    public static final String PROJECT_COLLECTION_ARTIFACT_TYPE = "application/vnd.arconia.project.collection.v1";

}
