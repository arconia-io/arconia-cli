package io.arconia.cli.project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import land.oras.Annotations;
import land.oras.ArtifactType;
import land.oras.ContainerRef;
import land.oras.Manifest;
import land.oras.Registry;

import io.arconia.cli.artifact.ArtifactInfo;
import io.arconia.cli.artifact.ArtifactPublisher;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.project.oci.ProjectAnnotations;
import io.arconia.cli.project.oci.ProjectConfig;
import io.arconia.cli.project.oci.ProjectConfigParser;
import io.arconia.cli.project.oci.ProjectMediaTypes;
import io.arconia.cli.utils.IoUtils;

/**
 * Publishes a Project as an OCI Artifact to an OCI-compliant registry.
 */
public final class ProjectPublisher {

    private final OutputOptions outputOptions;
    private final Registry registry;

    public ProjectPublisher(Registry registry, OutputOptions outputOptions) {
        Assert.notNull(registry, "registry cannot be null");
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        this.outputOptions = outputOptions;
        this.registry = registry;
    }

    /**
     * Publish a single project to the registry.
     */
    public ProjectPushReport publish(ProjectPushArguments arguments) throws IOException {
        Assert.notNull(arguments, "arguments cannot be null");
        return publish(arguments, ProjectConfigParser.parseFromDirectory(arguments.projectPath()));
    }

    /**
     * Publish a single project to the registry.
     */
    public ProjectPushReport publish(ProjectPushArguments arguments, ProjectConfig projectConfig) throws IOException {
        Assert.notNull(arguments, "arguments cannot be null");
        Assert.notNull(projectConfig, "projectConfig cannot be null");

        Map<String, String> projectAnnotations = ProjectAnnotations.computeAnnotations(projectConfig, arguments.projectPath(), arguments.tag(), arguments.annotations());

        outputOptions.info("Publishing project '%s'...".formatted(projectConfig.name()));

        ArtifactPublisher artifactPublisher = new ArtifactPublisher(registry);
        ContainerRef containerRef = ContainerRef.parse(arguments.ref()).withTag(arguments.tag());
        Manifest manifest = artifactPublisher.publish(ArtifactInfo.builder()
                .config(projectConfig.toConfig())
                .annotations(Annotations.ofManifest(projectAnnotations))
                .path(arguments.projectPath())
                .containerRef(containerRef)
                .artifactType(ArtifactType.from(ProjectMediaTypes.PROJECT_ARTIFACT_TYPE))
                .contentType(ProjectMediaTypes.PROJECT_ARTIFACT_CONTENT_LAYER)
                .build());

        outputOptions.info("Published project '%s'".formatted(projectConfig.name()));
        outputOptions.info("OCI Artifact: %s".formatted(containerRef));
        outputOptions.info("Digest: %s".formatted(manifest.getDigest()));

        ProjectPushReport report = ProjectPushReport.builder()
                .artifact(ProjectPushReport.ArtifactEntry.builder()
                        .name(projectConfig.name())
                        .description(projectConfig.description())
                        .type(projectConfig.type())
                        .license(projectConfig.license())
                        .labels(projectConfig.labels())
                        .ref(containerRef.toString())
                        .tag(arguments.tag())
                        .digest(manifest.getDigest())
                        .build())
                .build();

        if (StringUtils.hasText(arguments.reportFileName())) {
            Path reportPath = Path.of(arguments.reportFileName()).toAbsolutePath();
            report.save(reportPath);
            outputOptions.info("Report: %s".formatted(reportPath));
        }

        return report;
    }

    /**
     * Publish all projects under the given path.
     */
    public ProjectPushReport publish(ProjectBatchPushArguments arguments) throws IOException {
        Assert.notNull(arguments, "arguments cannot be null");

        List<Path> projectPaths = IoUtils.discoverSubDirectoriesWithFile(arguments.projectPath(), ProjectConfigParser.CONFIG_FILE_NAME);

        outputOptions.newLine();
        if (projectPaths.isEmpty()) {
            throw new CliException("No project directories found under: %s".formatted(arguments.projectPath()));
        }

        outputOptions.info("Discovered %d project(s) under %s".formatted(projectPaths.size(), arguments.projectPath()));

        List<ProjectPushReport> reports = new ArrayList<>();
        for (Path projectPath : projectPaths) {
            outputOptions.newLine();
            ProjectConfig projectConfig = ProjectConfigParser.parseFromDirectory(projectPath);
            ProjectPushArguments projectPushArguments = ProjectPushArguments.builder()
                    .ref("%s/%s".formatted(arguments.baseRef(), projectConfig.name()))
                    .tag(arguments.tag())
                    .projectPath(projectPath)
                    .annotations(arguments.annotations())
                    .build();
            reports.add(publish(projectPushArguments, projectConfig));
        }

        ProjectPushReport report = ProjectPushReport.builder()
                .artifacts(reports.stream().map(ProjectPushReport::artifacts).flatMap(List::stream).toList())
                .build();

        if (StringUtils.hasText(arguments.reportFileName())) {
            Path reportPath = Path.of(arguments.reportFileName()).toAbsolutePath();
            report.save(reportPath);
            outputOptions.newLine();
            outputOptions.info("Report: %s".formatted(reportPath));
        }

        return report;
    }

}
