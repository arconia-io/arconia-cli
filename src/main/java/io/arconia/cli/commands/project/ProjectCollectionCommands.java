package io.arconia.cli.commands.project;

import java.io.IOException;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import land.oras.Registry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.artifact.ArtifactRegistry;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.commands.options.RegistryOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.project.ProjectPushReport;
import io.arconia.cli.project.collection.ProjectCollectionPublisher;
import io.arconia.cli.project.collection.ProjectCollectionPushArguments;
import io.arconia.cli.project.collection.ProjectCollectionPushReport;
import io.arconia.cli.project.collection.service.ProjectCollectionRegistry;
import io.arconia.cli.project.collection.service.ProjectCollectionService;

@Component
@Command(
        name = "collection",
        description = "Manage Java project collections."
)
public class ProjectCollectionCommands implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "add", description = "Register a Java project collection with the Arconia CLI configuration.")
    public void add(
            @Option(names = {"--name"}, required = true, description = "The name of the Java project collection (e.g. arconia-project-templates).") String collectionName,
            @Option(names = {"--ref"}, required = true, description = "The OCI artifact reference for the collection (e.g. ghcr.io/arconia-io/arconia-project-templates/collection).") String collectionRef,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) {
        try {
            Registry ociRegistry = ArtifactRegistry.create(registryOptions);
            ProjectCollectionService collectionService = new ProjectCollectionService(ociRegistry, outputOptions);
            collectionService.addCollection(collectionName, collectionRef);
        }
        catch (IOException e) {
            throw new CliException("Failed to add collection: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "list", description = "List available Java projects in a collection. Uses --name for a specific collection or lists all projects from all registered collections.")
    public void list(
            @Option(names = {"--name"}, description = "The name of a registered collection for which to list the Java projects.") @Nullable String collectionName,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) throws IOException {
        Registry ociRegistry = ArtifactRegistry.create(registryOptions);
        ProjectCollectionService collectionService = new ProjectCollectionService(ociRegistry, outputOptions);

        if (StringUtils.hasText(collectionName)) {
            collectionService.listCollection(collectionName);
        }
        else {
            var registry = ProjectCollectionRegistry.load();
            if (registry.collections().isEmpty()) {
                outputOptions.info("No project collections registered. Run 'arconia project collection add' to register one.");
                return;
            }
            for (var entry : registry.collections()) {
                outputOptions.newLine();
                collectionService.listCollection(entry.name());
            }
        }
    }

    @Command(name = "update", description = "Update a registered collection or all registered collections.")
    public void update(
            @Option(names = {"--name"}, description = "The name of a specific Java project collection to update. If omitted, updates all registered collections.") @Nullable String collectionName,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) throws IOException {
        Registry ociRegistry = ArtifactRegistry.create(registryOptions);
        ProjectCollectionService collectionService = new ProjectCollectionService(ociRegistry, outputOptions);

        if (StringUtils.hasText(collectionName)) {
            collectionService.updateCollection(collectionName);
        } else {
            var registry = ProjectCollectionRegistry.load();
            if (registry.collections().isEmpty()) {
                outputOptions.info("No project collections registered. Run 'arconia project collection add' to register one.");
                return;
            }
            for (var entry : registry.collections()) {
                outputOptions.newLine();
                collectionService.updateCollection(entry.name());
            }
        }
    }

    @Command(name = "remove", description = "Remove a Java project collection from the Arconia CLI configuration.")
    public void remove(
            @Option(names = {"--name"}, required = true, description = "The name of the collection to remove.") String collectionName,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) {
        try {
            Registry ociRegistry = ArtifactRegistry.create(registryOptions);
            ProjectCollectionService collectionService = new ProjectCollectionService(ociRegistry, outputOptions);
            collectionService.removeCollection(collectionName);
        }
        catch (IOException e) {
            throw new CliException("Failed to remove collection: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "push", description = "Publish a Java project collection as an OCI artifact.")
    public void push(
            @Option(names = {"--ref"}, required = true, description = "The full OCI artifact reference for a Java project collection (e.g. ghcr.io/arconia-io/arconia-project-templates/collection).") String ref,
            @Option(names = {"--name"}, required = true, description = "The name of the collection (e.g. arconia-project-templates).") String name,
            @Option(names = {"--description"}, defaultValue = "A collection of Java projects published as OCI Artifacts and managed by the Arconia CLI.", description = "A short description of the collection.") String description,
            @Option(names = {"--tag"}, defaultValue = "latest", description = "The version tag (e.g. 'latest', semantic version number, commit sha). Defaults to 'latest'.") String tag,
            @Option(names = {"--from-report"}, arity = "0..1", fallbackValue = ProjectPushReport.DEFAULT_FILENAME, description = "Path to a push report file (from 'arconia project push --output-report'). Defaults to '" + ProjectPushReport.DEFAULT_FILENAME + "' when specified without a path.") @Nullable String fromReport,
            @Option(names = {"--project"}, arity = "0..*", description = "Explicit project OCI references to include (e.g. --project ghcr.io/arconia-io/arconia-project-templates/data).") List<String> projectRefs,
            @Option(names = {"--annotation"}, arity = "0..*", description = "Additional annotations in key=value format (e.g. --annotation org.opencontainers.image.vendor=arconia).") @Nullable List<String> annotations,
            @Option(names = {"--output-report"}, arity = "0..1", fallbackValue = ProjectCollectionPushReport.DEFAULT_FILENAME, description = "Write a publish report file. Defaults to '" + ProjectCollectionPushReport.DEFAULT_FILENAME + "' when specified without a path.") @Nullable String reportFileName,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) throws IOException {
        if (!StringUtils.hasText(fromReport) && CollectionUtils.isEmpty(projectRefs)) {
            throw new CliException("At least one of --from-report or --project must be provided.");
        }

        ProjectCollectionPublisher publisher = new ProjectCollectionPublisher(ArtifactRegistry.create(registryOptions), outputOptions);

        publisher.publish(ProjectCollectionPushArguments.builder()
            .ref(ref)
            .tag(tag)
            .name(name)
            .description(description)
            .annotations(ArtifactAnnotations.parseAnnotations(annotations))
            .fromReport(fromReport)
            .projects(projectRefs)
            .reportFileName(reportFileName)
            .build());
    }

}
