package io.arconia.cli.project.collection.oci;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.project.collection.ProjectCollectionPushArguments;
import io.arconia.cli.utils.DateTimeUtils;
import io.arconia.cli.utils.GitUtils;

/**
 * Manage annotations for Project Collections OCI artifacts.
 */
public final class ProjectCollectionAnnotations {

    private ProjectCollectionAnnotations() {}

    public static Map<String, String> computeAnnotations(Path projectDirectory, ProjectCollectionPushArguments arguments) {
        Map<String, String> annotations = new LinkedHashMap<>();

        annotations.put(ArtifactAnnotations.OCI_CREATED, DateTimeUtils.nowIso());

        annotations.put(ArtifactAnnotations.OCI_TITLE, arguments.name());
        annotations.put(ArtifactAnnotations.OCI_DESCRIPTION, arguments.description());
        annotations.put(ArtifactAnnotations.OCI_VERSION, arguments.tag());

        String remoteUrl = GitUtils.getRemoteUrl(projectDirectory);
        if (StringUtils.hasText(remoteUrl)) {
            annotations.put(ArtifactAnnotations.OCI_SOURCE, remoteUrl);
        }
        String revision = GitUtils.getRevision(projectDirectory);
        if (StringUtils.hasText(revision)) {
            annotations.put(ArtifactAnnotations.OCI_REVISION, revision);
        }

        if (!CollectionUtils.isEmpty(arguments.annotations())) {
            annotations.putAll(arguments.annotations());
        }

        return annotations;
    }

}
