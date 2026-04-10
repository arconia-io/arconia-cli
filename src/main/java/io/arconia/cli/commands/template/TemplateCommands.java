package io.arconia.cli.commands.template;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import land.oras.Registry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.artifact.ArtifactRegistry;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.commands.options.RegistryOptions;
import io.arconia.cli.project.ProjectBatchPushArguments;
import io.arconia.cli.project.ProjectPublisher;
import io.arconia.cli.project.ProjectPushArguments;
import io.arconia.cli.project.ProjectPushReport;
import io.arconia.cli.project.catalog.service.ProjectCatalogRegistry;
import io.arconia.cli.project.catalog.service.ProjectCatalogService;
import io.arconia.cli.utils.IoUtils;

@Component
@Command(
        name = "template",
        description = "Manage and publish project templates.",
        subcommands = {TemplateCatalogCommands.class}
)
public class TemplateCommands implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "list", description = "List available project templates from the registered catalogs.")
    public void list(
            @Option(names = {"--name"}, description = "The name of the registered catalog for which to list the project templates.") @Nullable String catalogName,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) throws IOException {
        Registry ociRegistry = ArtifactRegistry.create(registryOptions);
        ProjectCatalogService catalogService = new ProjectCatalogService(ociRegistry, outputOptions);

        if (StringUtils.hasText(catalogName)) {
            catalogService.listCatalog(catalogName);
        } else {
            catalogService.ensureBuiltInCatalogRegistered();
            var registry = ProjectCatalogRegistry.load();
            if (registry.catalogs().isEmpty()) {
                outputOptions.info("No template catalogs registered. Run 'arconia template catalog add' to register one.");
                return;
            }
            for (var entry : registry.catalogs()) {
                outputOptions.newLine();
                catalogService.listCatalog(entry.name());
            }
        }
    }

    @Command(name = "push", description = "Publish a project template as an OCI artifact.")
    public void push(
            @Option(names = {"--ref"}, description = "The full OCI artifact reference for a single project template (e.g. ghcr.io/arconia-io/arconia-templates/server-http). Mutually exclusive with --base-ref.") @Nullable String ref,
            @Option(names = {"--base-ref"}, description = "The base OCI reference for all discovered project templates (e.g. ghcr.io/arconia-io/arconia-templates). Discovers and pushes all templates found as direct subdirectories of --path. Mutually exclusive with --ref.") @Nullable String baseRef,
            @Option(names = {"--path"}, description = "Path to the template directory, or with --base-ref: the parent directory to discover templates in. Defaults to the current working directory.") @Nullable String path,
            @Option(names = {"--tag"}, defaultValue = "latest", description = "The version tag (e.g. 'latest', semantic version number, commit sha). Defaults to 'latest'.") String tag,
            @Option(names = {"--annotation"}, arity = "0..*", description = "Additional annotations in key=value format (e.g. --annotation org.opencontainers.image.vendor=arconia).") @Nullable List<String> annotations,
            @Option(names = {"--output-report"}, arity = "0..1", fallbackValue = ProjectPushReport.DEFAULT_FILENAME, description = "Write a publish report file. Defaults to '" + ProjectPushReport.DEFAULT_FILENAME + "' when specified without a path.") @Nullable String reportFileName,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) throws IOException {
        if (StringUtils.hasText(ref) == StringUtils.hasText(baseRef)) {
            throw new ParameterException(spec.commandLine(), "exactly one of --ref or --base-ref must be specified");
        }

        Path projectPath = IoUtils.getProjectPath(path);
        ProjectPublisher projectPublisher = new ProjectPublisher(ArtifactRegistry.create(registryOptions), outputOptions);

        if (StringUtils.hasText(ref)) {
            projectPublisher.publish(ProjectPushArguments.builder()
                    .ref(ref)
                    .tag(tag)
                    .annotations(ArtifactAnnotations.parseAnnotations(annotations))
                    .projectPath(projectPath)
                    .reportFileName(reportFileName)
                    .build());
        } else if (StringUtils.hasText(baseRef)) {
            projectPublisher.publish(ProjectBatchPushArguments.builder()
                    .baseRef(baseRef)
                    .tag(tag)
                    .annotations(ArtifactAnnotations.parseAnnotations(annotations))
                    .projectPath(projectPath)
                    .reportFileName(reportFileName)
                    .build());
        }
    }

}
