package io.arconia.cli.commands.template;

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
import io.arconia.cli.project.catalog.ProjectCatalogPublisher;
import io.arconia.cli.project.catalog.ProjectCatalogPushArguments;
import io.arconia.cli.project.catalog.ProjectCatalogPushReport;
import io.arconia.cli.project.catalog.service.ProjectCatalogRegistry;
import io.arconia.cli.project.catalog.service.ProjectCatalogService;

@Component
@Command(
        name = "catalog",
        description = "Manage catalogs of project templates."
)
public class TemplateCatalogCommands implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "add", description = "Register a template catalog with the Arconia CLI configuration.")
    public void add(
            @Option(names = {"--name"}, required = true, description = "The name of the template catalog (e.g. arconia-project-templates).") String catalogName,
            @Option(names = {"--ref"}, required = true, description = "The OCI artifact reference for the catalog (e.g. ghcr.io/arconia-io/arconia-templates/catalog).") String catalogRef,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) {
        try {
            Registry ociRegistry = ArtifactRegistry.create(registryOptions);
            ProjectCatalogService catalogService = new ProjectCatalogService(ociRegistry, outputOptions);
            catalogService.addCatalog(catalogName, catalogRef);
        }
        catch (IOException e) {
            throw new CliException("Failed to add catalog: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "update", aliases = "upgrade", description = "Update a registered template catalog or all registered catalogs.")
    public void update(
            @Option(names = {"--name"}, description = "The name of a specific template catalog to update. If omitted, updates all registered catalogs.") @Nullable String catalogName,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) throws IOException {
        Registry ociRegistry = ArtifactRegistry.create(registryOptions);
        ProjectCatalogService catalogService = new ProjectCatalogService(ociRegistry, outputOptions);

        if (StringUtils.hasText(catalogName)) {
            catalogService.updateCatalog(catalogName);
        } else {
            catalogService.ensureBuiltInCatalogRegistered();
            var registry = ProjectCatalogRegistry.load();
            if (registry.catalogs().isEmpty()) {
                outputOptions.info("No template catalogs registered. Run 'arconia template catalog add' to register one.");
                return;
            }
            for (var entry : registry.catalogs()) {
                outputOptions.newLine();
                catalogService.updateCatalog(entry.name());
            }
        }
    }

    @Command(name = "remove", aliases = "rm", description = "Remove a template catalog from the Arconia CLI configuration.")
    public void remove(
            @Option(names = {"--name"}, required = true, description = "The name of the catalog to remove.") String catalogName,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) {
        try {
            Registry ociRegistry = ArtifactRegistry.create(registryOptions);
            ProjectCatalogService catalogService = new ProjectCatalogService(ociRegistry, outputOptions);
            catalogService.removeCatalog(catalogName);
        }
        catch (IOException e) {
            throw new CliException("Failed to remove catalog: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "push", description = "Publish a template catalog as an OCI artifact.")
    public void push(
            @Option(names = {"--ref"}, required = true, description = "The full OCI artifact reference for a template catalog (e.g. ghcr.io/arconia-io/arconia-templates/catalog).") String ref,
            @Option(names = {"--name"}, required = true, description = "The name of the catalog (e.g. arconia-templates).") String name,
            @Option(names = {"--description"}, defaultValue = "A catalog of Java project templates published as OCI Artifacts and managed by the Arconia CLI.", description = "A short description of the catalog.") String description,
            @Option(names = {"--tag"}, defaultValue = "latest", description = "The version tag (e.g. 'latest', semantic version number, commit sha). Defaults to 'latest'.") String tag,
            @Option(names = {"--from-report"}, arity = "0..1", fallbackValue = ProjectPushReport.DEFAULT_FILENAME, description = "Path to a push report file (from 'arconia template push --output-report'). Defaults to '" + ProjectPushReport.DEFAULT_FILENAME + "' when specified without a path.") @Nullable String fromReport,
            @Option(names = {"--template"}, arity = "0..*", description = "Explicit template OCI references to include (e.g. --template ghcr.io/arconia-io/arconia-templates/data).") List<String> projectRefs,
            @Option(names = {"--annotation"}, arity = "0..*", description = "Additional annotations in key=value format (e.g. --annotation org.opencontainers.image.vendor=arconia).") @Nullable List<String> annotations,
            @Option(names = {"--output-report"}, arity = "0..1", fallbackValue = ProjectCatalogPushReport.DEFAULT_FILENAME, description = "Write a push report file. Defaults to '" + ProjectCatalogPushReport.DEFAULT_FILENAME + "' when specified without a path.") @Nullable String reportFileName,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) throws IOException {
        if (!StringUtils.hasText(fromReport) && CollectionUtils.isEmpty(projectRefs)) {
            throw new CliException("At least one of --from-report or --template must be provided.");
        }

        ProjectCatalogPublisher publisher = new ProjectCatalogPublisher(ArtifactRegistry.create(registryOptions), outputOptions);

        publisher.publish(ProjectCatalogPushArguments.builder()
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
