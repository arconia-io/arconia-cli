package io.arconia.cli.commands;

import java.util.Arrays;
import java.util.List;

import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.openrewrite.OpenRewriteRunner;
import io.arconia.cli.openrewrite.RecipeProvider;
import io.arconia.cli.openrewrite.UpdateOptions;
import io.arconia.cli.openrewrite.recipes.GradleRecipe;
import io.arconia.cli.openrewrite.recipes.ArconiaRecipe;
import io.arconia.cli.openrewrite.recipes.MavenRecipe;
import io.arconia.cli.openrewrite.recipes.SpringAiRecipe;

import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandRegistration.OptionArity;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(command = "update", alias = "upgrade", group = "Update")
public class UpdateCommands {

    @Command(command = "gradle", description = "Update project to new Gradle version.")
    public void updateGradle(
        CommandContext commandContext,
        @Option(description = "Update in dry-run mode.") boolean dryRun,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output about the execution.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(description = "Additional build parameters passed directly to the build tool.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var openRewriteRunner = new OpenRewriteRunner(terminal);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(GradleRecipe.RECIPE_NAME)
            .rewriteRecipeLibrary(GradleRecipe.RECIPE_LIBRARY)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        openRewriteRunner.update(updateOptions, RecipeProvider.OPENREWRITE);
    }

    @Command(command = "maven", description = "Update project to new Maven version.")
    public void updateMaven(
        CommandContext commandContext,
        @Option(description = "Update in dry-run mode.") boolean dryRun,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output about the execution.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(description = "Additional build parameters passed directly to the build tool.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var openRewriteRunner = new OpenRewriteRunner(terminal);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(MavenRecipe.RECIPE_NAME)
            .rewriteRecipeLibrary(MavenRecipe.RECIPE_LIBRARY)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        openRewriteRunner.update(updateOptions, RecipeProvider.OPENREWRITE);
    }

    @Command(command = "framework", description = "Update project to new Arconia Framework version.")
    public void updateArconia(
        CommandContext commandContext,
        @Option(description = "Update in dry-run mode.") boolean dryRun,
        @Option(defaultValue = "0.11", description = "Arconia target version.") String toVersion,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output about the execution.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(description = "Additional build parameters passed directly to the build tool.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var openRewriteRunner = new OpenRewriteRunner(terminal);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(ArconiaRecipe.computeRecipeLibrary(toVersion))
            .rewriteRecipeLibrary(ArconiaRecipe.RECIPE_LIBRARY)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        openRewriteRunner.update(updateOptions, RecipeProvider.ARCONIA);
    }

    @Command(command = "spring-ai", description = "Update project to new Spring AI version.")
    public void updateSpringAi(
        CommandContext commandContext,
        @Option(description = "Update in dry-run mode.") boolean dryRun,
        @Option(defaultValue = "1.0", description = "Spring AI target version.") String toVersion,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output about the execution.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(description = "Additional build parameters passed directly to the build tool.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var openRewriteRunner = new OpenRewriteRunner(terminal);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(SpringAiRecipe.computeRecipeLibrary(toVersion))
            .rewriteRecipeLibrary(SpringAiRecipe.RECIPE_LIBRARY)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        openRewriteRunner.update(updateOptions, RecipeProvider.ARCONIA);
    }

}
