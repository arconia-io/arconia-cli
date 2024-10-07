package io.arconia.cli.commands;

import java.util.Arrays;
import java.util.List;

import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.openrewrite.OpenRewriteRunner;
import io.arconia.cli.openrewrite.UpdateOptions;
import io.arconia.cli.openrewrite.recipes.GradleRecipe;
import io.arconia.cli.openrewrite.recipes.JavaRecipe;
import io.arconia.cli.openrewrite.recipes.MavenRecipe;
import io.arconia.cli.openrewrite.recipes.SpringBootRecipe;

import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandRegistration.OptionArity;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(command = "update", alias = "upgrade", group = "Migration")
public class UpdateCommands {

    @Command(command = "gradle", description = "Update project to new Gradle version.")
    public void updateGradle(
        CommandContext commandContext,
        @Option(description = "Update in dry-run mode.") boolean dryRun,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(required = false, description = "Additional build parameters.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var openRewriteRunner = new OpenRewriteRunner(terminal);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(GradleRecipe.RECIPE_NAME)
            .rewriteRecipeLibrary(GradleRecipe.RECIPE_LIBRARY)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        openRewriteRunner.update(updateOptions);
    }

    @Command(command = "java", description = "Update project to new Java version.")
    public void updateJava(
        CommandContext commandContext,
        @Option(description = "Update in dry-run mode.") boolean dryRun,
        @Option(defaultValue = "21", description = "Java target version.") String toVersion,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(required = false, description = "Additional build parameters.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var openRewriteRunner = new OpenRewriteRunner(terminal);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(JavaRecipe.getRecipeName(toVersion))
            .rewriteRecipeLibrary(JavaRecipe.RECIPE_LIBRARY)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        openRewriteRunner.update(updateOptions);
    }

    @Command(command = "maven", description = "Update project to new Maven version.")
    public void updateMaven(
        CommandContext commandContext,
        @Option(description = "Update in dry-run mode.") boolean dryRun,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(required = false, description = "Additional build parameters.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var openRewriteRunner = new OpenRewriteRunner(terminal);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(MavenRecipe.RECIPE_NAME)
            .rewriteRecipeLibrary(MavenRecipe.RECIPE_LIBRARY)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        openRewriteRunner.update(updateOptions);
    }

    @Command(command = "spring-boot", description = "Update project to new Spring Boot version.")
    public void updateSpringBoot(
        CommandContext commandContext,
        @Option(description = "Update in dry-run mode.") boolean dryRun,
        @Option(defaultValue = "3.3", description = "Spring Boot target version.") String toVersion,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(required = false, description = "Additional build parameters.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var openRewriteRunner = new OpenRewriteRunner(terminal);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(SpringBootRecipe.getRecipeName(toVersion))
            .rewriteRecipeLibrary(SpringBootRecipe.RECIPE_LIBRARY)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        openRewriteRunner.update(updateOptions);
    }

}
