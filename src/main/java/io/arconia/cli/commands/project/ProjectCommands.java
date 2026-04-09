package io.arconia.cli.commands.project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
import io.arconia.cli.project.ProjectCreateArguments;
import io.arconia.cli.project.ProjectCreator;
import io.arconia.cli.project.ProjectPublisher;
import io.arconia.cli.project.ProjectPushArguments;
import io.arconia.cli.project.ProjectPushReport;
import io.arconia.cli.utils.IoUtils;

@Command(name = "project", description = "Create and manage Java projects", subcommands = {ProjectCollectionCommands.class})
@Component
public class ProjectCommands implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "create", description = "Create a new Java project")
    public void create(
            @Option(names = {"--name"}, description = "The name of the project to create.") String name,
            @Option(names = {"--template"}, description = "The project to use as a template (e.g. 'ghcr.io/arconia-io/arconia-project-templates/server-http').") String template,
            @Option(names = {"--group"}, defaultValue = "com.example", description = "The group ID for the project. If not provided, defaults to 'com.example'.") String group,
            @Option(names = {"--description"}, description = "The description for the project") String description,
            @Option(names = {"--package-name"}, description = "The package name for the project") String packageName,
            @Option(names = {"--path"}, description = "The path where the project will be created. Defaults to the current working directory.") String path,
            @Mixin RegistryOptions registryOptions,
            @Mixin OutputOptions outputOptions
    ) throws IOException {
        ProjectCreator projectCreator = new ProjectCreator(ArtifactRegistry.create(registryOptions), outputOptions);

        ProjectCreateArguments arguments = ProjectCreateArguments.builder()
                .name(name)
                .group(group)
                .description(description)
                .packageName(packageName)
                .build();

        projectCreator.create(arguments, template, IoUtils.getProjectPath(path));
    }

    @Command(name = "push", description = "Publish a Java project as an OCI artifact")
    public void push(
            @Option(names = {"--ref"}, description = "The full OCI artifact reference for a single Java project (e.g. ghcr.io/arconia-io/arconia-project-templates/web). Mutually exclusive with --base-ref.") @Nullable String ref,
            @Option(names = {"--base-ref"}, description = "The base OCI reference for all discovered projects (e.g. ghcr.io/arconia-io/arconia-project-templates). Discovers and pushes all projects found as direct subdirectories of --path. Mutually exclusive with --ref.") @Nullable String baseRef,
            @Option(names = {"--path"}, description = "Path to the project directory, or with --base-ref: the parent directory to discover projects in. Defaults to the current working directory.") @Nullable String path,
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
