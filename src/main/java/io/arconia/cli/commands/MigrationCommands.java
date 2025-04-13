package io.arconia.cli.commands;

import java.util.Arrays;
import java.util.List;

import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.openrewrite.RewriteOptions;
import io.arconia.cli.openrewrite.OpenRewriteRunner;

import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandRegistration.OptionArity;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.util.StringUtils;

@Command(group = "Migration")
public class MigrationCommands {

    @Command(command = "rewrite", description = "Run an OpenRewrite recipe.")
    public void rewrite(
        CommandContext commandContext,
        @Option(description = "Update in dry-run mode.") boolean dryRun,
        @Option(description = "Name of the OpenRewrite recipe to run. Example: 'io.arconia.rewrite.spring.ai.UpgradeSpringAi_1_0'.") String recipeName,
        @Option(required = false, description = "Maven coordinates of the library containing the OpenRewrite recipe to run. If not provided, the OpenRewrite OSS core recipe library will be used. Example: 'io.arconia.migrations:rewrite-arconia'.") String recipeLibrary,
        @Option(required = false, description = "Version of the library containing the OpenRewrite recipe to run. If not provided, the latest available version will be used. Example: '4.2.0'.") String recipeVersion,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(required = false, description = "Additional build parameters.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var openRewriteRunner = new OpenRewriteRunner(terminal);
        var migrateOptionsBuilder = RewriteOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(recipeName)
            .params(params != null ? Arrays.asList(params) : List.of());
        
        if (StringUtils.hasText(recipeLibrary)) {
            migrateOptionsBuilder.rewriteRecipeLibrary(recipeLibrary);
        }

        if (StringUtils.hasText(recipeVersion)) {
            migrateOptionsBuilder.rewriteRecipeVersion(recipeVersion);
        }

        openRewriteRunner.rewrite(migrateOptionsBuilder.build());
    }

}
