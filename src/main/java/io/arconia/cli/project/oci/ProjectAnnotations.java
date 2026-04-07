package io.arconia.cli.project.oci;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.utils.DateTimeUtils;
import io.arconia.cli.utils.GitUtils;

/**
 * Manage annotations for Projects OCI artifacts.
 */
public final class ProjectAnnotations {

    private ProjectAnnotations() {}

    public static final String PROJECT_TYPE = "io.arconia.project.type";

    public static final String PROJECT_LABELS = "io.arconia.project.labels";

    public static Map<String, String> computeAnnotations(ProjectConfig projectConfig, Path projectDirectory, String tag, Map<String, String> additionalAnnotations) {
        Map<String, String> annotations = new LinkedHashMap<>();

        annotations.put(ArtifactAnnotations.OCI_CREATED, DateTimeUtils.nowIso());

        annotations.put(ArtifactAnnotations.OCI_TITLE, projectConfig.name());
        annotations.put(ArtifactAnnotations.OCI_DESCRIPTION, projectConfig.description());
        annotations.put(ArtifactAnnotations.OCI_VERSION, tag);
        annotations.put(ArtifactAnnotations.OCI_LICENSES, projectConfig.license());

        String remoteUrl = GitUtils.getRemoteUrl(projectDirectory);
        if (StringUtils.hasText(remoteUrl)) {
            annotations.put(ArtifactAnnotations.OCI_SOURCE, remoteUrl);
        }
        String revision = GitUtils.getRevision(projectDirectory);
        if (StringUtils.hasText(revision)) {
            annotations.put(ArtifactAnnotations.OCI_REVISION, revision);
        }

        annotations.put(PROJECT_TYPE, projectConfig.type());
        annotations.put(PROJECT_LABELS, String.join(",", projectConfig.labels()));

        if (!CollectionUtils.isEmpty(additionalAnnotations)) {
            annotations.putAll(additionalAnnotations);
        }

        return annotations;
    }

}
