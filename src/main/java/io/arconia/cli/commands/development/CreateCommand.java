package io.arconia.cli.commands.development;

import java.io.IOException;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import io.arconia.cli.artifact.ArtifactRegistry;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.commands.options.RegistryOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.project.ProjectCreateArguments;
import io.arconia.cli.project.ProjectCreator;
import io.arconia.cli.utils.IoUtils;

@Component
@Command(name = "create", description = "Create a new Java project from a template.")
public class CreateCommand implements Runnable {

    @Option(names = {"--name"}, description = "The name of the project to create.")
    String name;

    @Option(names = {"--template"}, description = "The project template to use (e.g. 'ghcr.io/arconia-io/arconia-templates/server-http' or 'server-http').")
    String template;

    @Option(names = {"--group"}, defaultValue = "com.example", description = "The group ID for the project. If not provided, defaults to 'com.example'.")
    @Nullable
    String group;

    @Option(names = {"--description"}, description = "The description for the project.")
    @Nullable
    String description;

    @Option(names = {"--package-name"}, description = "The package name for the project.")
    @Nullable
    String packageName;

    @Option(names = {"--path"}, description = "The path where the project will be created. Defaults to the current working directory.")
    @Nullable
    String path;

    @Mixin
    RegistryOptions registryOptions;

    @Mixin
    OutputOptions outputOptions;

    @Override
    public void run() {
        try {
            ProjectCreator projectCreator = new ProjectCreator(ArtifactRegistry.create(registryOptions), outputOptions);

            ProjectCreateArguments arguments = ProjectCreateArguments.builder()
                    .name(name)
                    .group(group)
                    .description(description)
                    .packageName(packageName)
                    .build();

            projectCreator.create(arguments, template, IoUtils.getProjectPath(path));
        }
        catch (IOException e) {
            throw new CliException("Failed to create project: %s".formatted(e.getMessage()), e);
        }
    }

}
