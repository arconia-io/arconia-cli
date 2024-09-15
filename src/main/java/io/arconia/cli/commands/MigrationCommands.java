package io.arconia.cli.commands;

import java.util.Arrays;
import java.util.List;

import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.openrewrite.UpdateOptions;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(group = "Migration")
public class MigrationCommands {

    private final BuildToolRunner buildToolRunner;

    public MigrationCommands() {
        this.buildToolRunner = BuildToolRunner.create();
    }

    @Command(command = "update", description = "Update project to new Spring Boot version.")
    public void build(
        @Option(description = "Update in dry-run mode.") boolean dryRun,
        @Option(defaultValue = "3.3", description = "Spring Boot target version.") String springBootVersion,
        @Option(required = false, description = "OpenRewrite plugin version.") String rewritePluginVersion,
        @Option(required = false, description = "OpenRewrite Spring Recipes version.") String springRecipesVersion,
        @Option(required = false, description = "Additional build parameters") String[] params
    ) {
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .springBootVersion(springBootVersion)
            .rewritePluginVersion(rewritePluginVersion)
            .springRecipesVersion(springRecipesVersion)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();
        buildToolRunner.update(updateOptions);
    }

}
