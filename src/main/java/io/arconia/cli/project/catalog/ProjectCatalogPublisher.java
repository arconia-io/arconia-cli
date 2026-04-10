package io.arconia.cli.project.catalog;

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
import io.arconia.cli.project.catalog.oci.ProjectCatalogAnnotations;
import io.arconia.cli.project.oci.ProjectAnnotations;
import io.arconia.cli.project.oci.ProjectMediaTypes;
import io.arconia.cli.utils.IoUtils;

/**
 * Publishes a Project Catalog as an OCI Image Index to an OCI-compliant registry.
 */
public final class ProjectCatalogPublisher {

    private final OutputOptions outputOptions;
    private final Registry registry;

    public ProjectCatalogPublisher(Registry registry, OutputOptions outputOptions) {
        Assert.notNull(registry, "registry cannot be null");
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        this.registry = registry;
        this.outputOptions = outputOptions;
    }

    /**
     * Publish a project catalog to the registry.
     */
    public ProjectCatalogPushReport publish(ProjectCatalogPushArguments arguments) throws IOException {
        Assert.notNull(arguments, "arguments cannot be null");

        outputOptions.info("Publishing catalog '%s'...".formatted(arguments.name()));
        ContainerRef containerRef = ContainerRef.parse(arguments.ref()).withTag(arguments.tag());

        Index catalogIndex;
        if (StringUtils.hasText(arguments.fromReport())) {
            Path reportPath = Path.of(arguments.fromReport()).toAbsolutePath();
            outputOptions.verbose("Loading template push report from: %s".formatted(reportPath));

            ProjectPushReport report = ProjectPushReport.load(reportPath);
            outputOptions.info("Loaded template push report with %d artifact(s).".formatted(report.artifacts().size()));

            List<ProjectCatalogEntry> entries = report.artifacts().stream()
                    .map(artifact -> ProjectCatalogEntry.builder()
                            .name(artifact.name())
                            .tag(artifact.tag())
                            .ref(artifact.ref())
                            .digest(artifact.digest())
                            .description(artifact.description())
                            .type(artifact.type())
                            .labels(artifact.labels())
                            .build())
                    .toList();

            catalogIndex = publishCatalog(arguments, entries, containerRef);
        } else if (!CollectionUtils.isEmpty(arguments.projects())) {
            outputOptions.info("Resolving %d template reference(s) from registry...".formatted(arguments.projects().size()));

            List<ProjectCatalogEntry> entries = arguments.projects().parallelStream()
                    .map(this::buildProjectCatalogEntryFromRef)
                    .toList();

            catalogIndex = publishCatalog(arguments, entries, containerRef);
        } else {
            throw new CliException("No templates specified for publishing in the catalog.");
        }

        outputOptions.newLine();
        outputOptions.info("Published catalog '%s'".formatted(arguments.name()));
        outputOptions.info("OCI Artifact: %s".formatted(containerRef));
        outputOptions.info("Digest: %s".formatted(catalogIndex.getDescriptor().getDigest()));

        ProjectCatalogPushReport report = ProjectCatalogPushReport.builder()
                .artifact(ProjectCatalogPushReport.ArtifactEntry.builder()
                        .name(arguments.name())
                        .description(arguments.description())
                        .ref(containerRef.toString())
                        .tag(arguments.tag())
                        .digest(catalogIndex.getDescriptor().getDigest())
                        .build())
                .build();

        if (StringUtils.hasText(arguments.reportFileName())) {
            Path reportPath = Path.of(arguments.reportFileName()).toAbsolutePath();
            report.save(reportPath);
            outputOptions.info("Report: %s".formatted(reportPath));
        }

        return report;
    }

    private Index publishCatalog(ProjectCatalogPushArguments arguments, List<ProjectCatalogEntry> entries, ContainerRef containerRef) {
        Path projectDirectory = IoUtils.getProjectPath();

        List<ManifestDescriptor> manifestDescriptors = entries.stream()
                .map(this::buildManifestDescriptor)
                .toList();

        Index index = Index.fromManifests(manifestDescriptors)
                .withArtifactType(ArtifactType.from(ProjectMediaTypes.PROJECT_CATALOG_ARTIFACT_TYPE))
                .withAnnotations(ProjectCatalogAnnotations.computeAnnotations(projectDirectory, arguments));

        return registry.pushIndex(containerRef, index);
    }

    private ManifestDescriptor buildManifestDescriptor(ProjectCatalogEntry entry) {
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

    private ProjectCatalogEntry buildProjectCatalogEntryFromRef(String ref) {
        ContainerRef projectContainerRef = ContainerRef.parse(ref);
        Manifest manifest = registry.getManifest(projectContainerRef);

        String digest = manifest.getDigest();
        Map<String, String> annotations = manifest.getAnnotations();

        String name = annotations.get(ArtifactAnnotations.OCI_TITLE);
        String description = annotations.get(ArtifactAnnotations.OCI_DESCRIPTION);
        String tag = annotations.get(ArtifactAnnotations.OCI_VERSION);
        String type = annotations.get(ProjectAnnotations.PROJECT_TYPE);
        String labels = annotations.get(ProjectAnnotations.PROJECT_LABELS);

        return ProjectCatalogEntry.builder()
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
