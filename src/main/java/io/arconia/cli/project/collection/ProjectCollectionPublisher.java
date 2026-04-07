package io.arconia.cli.project.collection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import land.oras.ArtifactType;
import land.oras.ContainerRef;
import land.oras.Index;
import land.oras.Manifest;
import land.oras.ManifestDescriptor;
import land.oras.Registry;

import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.artifact.ArtifactMediaTypes;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.project.ProjectPushReport;
import io.arconia.cli.project.collection.oci.ProjectCollectionAnnotations;
import io.arconia.cli.project.oci.ProjectAnnotations;
import io.arconia.cli.project.oci.ProjectMediaTypes;
import io.arconia.cli.utils.IoUtils;

/**
 * Publishes a Project Collection as an OCI Image Index to an OCI-compliant registry.
 */
public final class ProjectCollectionPublisher {

    private final OutputOptions outputOptions;
    private final Registry registry;

    public ProjectCollectionPublisher(Registry registry, OutputOptions outputOptions) {
        Assert.notNull(registry, "registry cannot be null");
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        this.registry = registry;
        this.outputOptions = outputOptions;
    }

    /**
     * Publish a project collection to the registry.
     */
    public ProjectCollectionPushReport publish(ProjectCollectionPushArguments arguments) throws IOException {
        Assert.notNull(arguments, "arguments cannot be null");

        outputOptions.info("Publishing project collection '%s'...".formatted(arguments.name()));
        ContainerRef containerRef = ContainerRef.parse(arguments.ref()).withTag(arguments.tag());

        Index collectionIndex;
        if (StringUtils.hasText(arguments.fromReport())) {
            Path reportPath = Path.of(arguments.fromReport()).toAbsolutePath();
            outputOptions.verbose("Loading project push report from: %s".formatted(reportPath));

            ProjectPushReport report = ProjectPushReport.load(reportPath);
            outputOptions.info("Loaded project push report with %d artifact(s).".formatted(report.artifacts().size()));

            List<ProjectCollectionEntry> entries = report.artifacts().stream()
                    .map(artifact -> ProjectCollectionEntry.builder()
                            .name(artifact.name())
                            .tag(artifact.tag())
                            .ref(artifact.ref())
                            .digest(artifact.digest())
                            .description(artifact.description())
                            .type(artifact.type())
                            .labels(artifact.labels())
                            .build())
                    .toList();

            collectionIndex = publishCollection(arguments, entries, containerRef);
        } else if (!CollectionUtils.isEmpty(arguments.projects())) {
            outputOptions.info("Resolving %d project reference(s) from registry...".formatted(arguments.projects().size()));

            List<ProjectCollectionEntry> entries = arguments.projects().parallelStream()
                    .map(this::buildProjectCollectionEntryFromRef)
                    .toList();

            collectionIndex = publishCollection(arguments, entries, containerRef);
        } else {
            throw new CliException("No projects specified for publishing in the collection.");
        }

        outputOptions.newLine();
        outputOptions.info("Published collection '%s'".formatted(arguments.name()));
        outputOptions.info("OCI Artifact: %s".formatted(containerRef));
        outputOptions.info("Digest: %s".formatted(collectionIndex.getDescriptor().getDigest()));

        ProjectCollectionPushReport report = ProjectCollectionPushReport.builder()
                .artifact(ProjectCollectionPushReport.ArtifactEntry.builder()
                        .name(arguments.name())
                        .description(arguments.description())
                        .ref(containerRef.toString())
                        .tag(arguments.tag())
                        .digest(collectionIndex.getDescriptor().getDigest())
                        .build())
                .build();

        if (StringUtils.hasText(arguments.reportFileName())) {
            Path reportPath = Path.of(arguments.reportFileName()).toAbsolutePath();
            report.save(reportPath);
            outputOptions.info("Report: %s".formatted(reportPath));
        }

        return report;
    }

    private Index publishCollection(ProjectCollectionPushArguments arguments, List<ProjectCollectionEntry> entries, ContainerRef containerRef) {
        Path projectDirectory = IoUtils.getProjectPath();

        List<ManifestDescriptor> manifestDescriptors = entries.stream()
                .map(this::buildManifestDescriptor)
                .toList();

        Index index = Index.fromManifests(manifestDescriptors)
                .withArtifactType(ArtifactType.from(ProjectMediaTypes.PROJECT_COLLECTION_ARTIFACT_TYPE))
                .withAnnotations(ProjectCollectionAnnotations.computeAnnotations(projectDirectory, arguments));

        return registry.pushIndex(containerRef, index);
    }

    private ManifestDescriptor buildManifestDescriptor(ProjectCollectionEntry entry) {
        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put(ArtifactAnnotations.OCI_TITLE, entry.name());
        annotations.put(ArtifactAnnotations.OCI_DESCRIPTION, entry.description());
        annotations.put(ArtifactAnnotations.OCI_VERSION, entry.tag());
        annotations.put(ArtifactAnnotations.OCI_REF_NAME, entry.ref());
        annotations.put(ProjectAnnotations.PROJECT_TYPE, entry.type());
        annotations.put(ProjectAnnotations.PROJECT_LABELS, String.join(",", entry.labels()));

        return ManifestDescriptor.of(ArtifactMediaTypes.IMAGE_MANIFEST, entry.digest(), 0)
                .withArtifactType(ProjectMediaTypes.PROJECT_ARTIFACT_TYPE)
                .withAnnotations(annotations);
    }

    private ProjectCollectionEntry buildProjectCollectionEntryFromRef(String ref) {
        ContainerRef projectContainerRef = ContainerRef.parse(ref);
        Manifest manifest = registry.getManifest(projectContainerRef);

        String digest = manifest.getDigest();
        Map<String, String> annotations = manifest.getAnnotations();

        String name = annotations.get(ArtifactAnnotations.OCI_TITLE);
        String description = annotations.get(ArtifactAnnotations.OCI_DESCRIPTION);
        String tag = annotations.get(ArtifactAnnotations.OCI_VERSION);
        String type = annotations.get(ProjectAnnotations.PROJECT_TYPE);
        String labels = annotations.get(ProjectAnnotations.PROJECT_LABELS);

        return ProjectCollectionEntry.builder()
                .ref(ref)
                .name(name)
                .description(description)
                .type(type)
                .labels(List.of(labels.split(",")))
                .tag(tag)
                .digest(digest)
                .build();
    }

}
