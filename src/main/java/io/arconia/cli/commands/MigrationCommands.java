package io.arconia.cli.commands;

import java.util.Arrays;
import java.util.List;

import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.openrewrite.UpdateOptions;

import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandRegistration.OptionArity;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(group = "Migration")
public class MigrationCommands {

    @Command(command = "update", description = "Update project to new Spring Boot version.")
    public void build(
        CommandContext commandContext,
        @Option(description = "Update in dry-run mode.") boolean dryRun,
        @Option(defaultValue = "3.3", description = "Spring Boot target version.") String springBootVersion,
        @Option(required = false, description = "OpenRewrite plugin version.") String rewritePluginVersion,
        @Option(required = false, description = "OpenRewrite Spring Recipes version.") String springRecipesVersion,
        @Option(required = false, description = "Additional build parameters.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var buildToolRunner = BuildToolRunner.create(terminal);
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
