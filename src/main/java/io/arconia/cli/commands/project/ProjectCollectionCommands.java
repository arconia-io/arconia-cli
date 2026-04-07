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
import io.arconia.cli.commands.options.CliTableFormatter;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.project.ProjectPushReport;
import io.arconia.cli.project.collection.ProjectCollectionPublisher;
import io.arconia.cli.project.collection.ProjectCollectionPushArguments;
import io.arconia.cli.project.collection.ProjectCollectionPushReport;
import io.arconia.cli.project.collection.service.ProjectCollectionCache;
import io.arconia.cli.project.collection.service.ProjectCollectionHeader;
import io.arconia.cli.project.collection.service.ProjectCollectionRegistry;
import io.arconia.cli.project.collection.service.ProjectCollectionService;
import io.arconia.cli.project.collection.service.ProjectCollectionSummary;

@Component
@Command(
        name = "collection",
        description = "Manage project collections."
)
public class ProjectCollectionCommands implements Runnable {

    private static final List<String> PROJECT_TABLE_HEADERS = List.of("NAME", "DESCRIPTION", "TYPE", "LABELS");

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "add", description = "Register a project collection with the Arconia CLI configuration.")
    public void add(
            @Option(names = {"--name"}, required = true, description = "A short alias for the collection (e.g. arconia-project-collection).") String collectionName,
            @Option(names = {"--ref"}, required = true, description = "The OCI artifact reference for the collection (e.g. ghcr.io/org/project-collection:4.2.0).") String collectionRef,
            @Mixin OutputOptions outputOptions
    ) {
        try {
            Registry ociRegistry = ArtifactRegistry.create();
            ProjectCollectionService collectionService = new ProjectCollectionService(ociRegistry, outputOptions);
            collectionService.addCollection(collectionName, collectionRef);
        }
        catch (IOException e) {
            throw new CliException("Failed to add collection: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "remove", description = "Remove a collection from the Arconia CLI configuration.")
    public void remove(
            @Option(names = {"--name"}, required = true, description = "The alias of the collection to remove.") String collectionName,
            @Mixin OutputOptions outputOptions
    ) {
        try {
            Registry ociRegistry = ArtifactRegistry.create();
            ProjectCollectionService collectionService = new ProjectCollectionService(ociRegistry, outputOptions);
            collectionService.removeCollection(collectionName);
        }
        catch (IOException e) {
            throw new CliException("Failed to remove collection: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "list", description = "List available projects in a collection. Uses --name for a specific collection or lists all projects from all registered collections.")
    public void list(
            @Option(names = {"--name"}, description = "The alias of a registered collection for which to list the projects.") @Nullable String collectionName,
            @Mixin OutputOptions outputOptions
    ) throws IOException {
        if (StringUtils.hasText(collectionName)) {
            var collection = ProjectCollectionRegistry.findCollection(collectionName);
            if (collection == null) {
                outputOptions.error("Collection '%s' is not registered. Run 'arconia project collection add' to register it first.".formatted(collectionName));
                return;
            }
            var index = ProjectCollectionCache.load(collectionName);
            if (index == null) {
                outputOptions.error("No cached data found for collection '%s'. Run 'arconia project collection add' to register it first.".formatted(collectionName));
                return;
            }
            printCollectionHeader(outputOptions, new ProjectCollectionHeader(collection.name(), collection.ref(), index.getAnnotations().get(ArtifactAnnotations.OCI_DESCRIPTION)));
            outputOptions.newLine();
            outputOptions.table(CliTableFormatter.format(outputOptions.colorScheme(), PROJECT_TABLE_HEADERS, toProjectRows(ProjectCollectionCache.toProjectSummaries(index)), 3));
        }
        else {
            var registry = ProjectCollectionRegistry.load();
            if (registry.collections().isEmpty()) {
                outputOptions.info("No project collections registered. Run 'arconia project collection add' to register one.");
                return;
            }
            for (var entry : registry.collections()) {
                var index = ProjectCollectionCache.load(entry.name());
                outputOptions.newLine();
                if (index == null) {
                    printCollectionHeader(outputOptions, new ProjectCollectionHeader(entry.name(), entry.ref(), null));
                    outputOptions.info("  No cached data. Run 'arconia project collection add --name %s' to refresh.".formatted(entry.name()));
                    continue;
                }
                printCollectionHeader(outputOptions, new ProjectCollectionHeader(entry.name(), entry.ref(), index.getAnnotations().get(ArtifactAnnotations.OCI_DESCRIPTION)));
                outputOptions.newLine();
                outputOptions.table(CliTableFormatter.format(outputOptions.colorScheme(), PROJECT_TABLE_HEADERS, toProjectRows(ProjectCollectionCache.toProjectSummaries(index)), 3));
            }
        }
    }

    private static void printCollectionHeader(OutputOptions outputOptions, ProjectCollectionHeader header) {
        outputOptions.info("📦 %s (%s)".formatted(header.name(), header.ref()));
        if (StringUtils.hasText(header.description())) {
            outputOptions.info("   %s".formatted(header.description()));
        }
    }

    private static List<List<String>> toProjectRows(List<ProjectCollectionSummary> summaries) {
        return summaries.stream()
                .map(s -> List.of(s.name(), s.description(), s.type(), String.join(", ", s.labels())))
                .toList();
    }

    @Command(name = "push", description = "Publish a project collection as an OCI artifact.")
    public void push(
            @Option(names = {"--ref"}, required = true, description = "The full OCI artifact reference for a project collection (e.g. ghcr.io/org/projects/collection).") String ref,
            @Option(names = {"--name"}, required = true, description = "The collection identifier (e.g. arconia-project-collection).") String name,
            @Option(names = {"--description"}, defaultValue = "A collection of projects published as OCI Artifacts and managed by the Arconia CLI.", description = "A short description of the collection.") String description,
            @Option(names = {"--tag"}, defaultValue = "latest", description = "The version tag (e.g. 'latest', semantic version number, commit sha). Defaults to 'latest'.") String tag,
            @Option(names = {"--from-report"}, arity = "0..1", fallbackValue = ProjectPushReport.DEFAULT_FILENAME, description = "Path to a push report file (from 'arconia project push --report'). Defaults to '" + ProjectPushReport.DEFAULT_FILENAME + "' when specified without a path.") @Nullable String fromReport,
            @Option(names = {"--project"}, arity = "0..*", description = "Explicit project OCI references to include (e.g. --project ghcr.io/org/projects/project:1.0.0).") List<String> projectRefs,
            @Option(names = {"--annotation"}, arity = "0..*", description = "Extra annotations in key=value format (e.g. --annotation org.opencontainers.image.vendor=arconia).") @Nullable List<String> annotations,
            @Option(names = {"--report"}, arity = "0..1", fallbackValue = ProjectCollectionPushReport.DEFAULT_FILENAME, description = "Write a publish report file. Defaults to '" + ProjectCollectionPushReport.DEFAULT_FILENAME + "' when specified without a path.") @Nullable String reportFileName,
            @Mixin OutputOptions outputOptions
    ) throws IOException {
        if (!StringUtils.hasText(fromReport) && CollectionUtils.isEmpty(projectRefs)) {
            throw new CliException("At least one of --from-report or --project must be provided.");
        }

        ProjectCollectionPublisher publisher = new ProjectCollectionPublisher(ArtifactRegistry.create(), outputOptions);

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
