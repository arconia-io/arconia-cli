package io.arconia.cli.commands.refactoring;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.commands.options.ParametersOption;
import io.arconia.cli.openrewrite.OpenRewriteRunner;
import io.arconia.cli.openrewrite.UpdateArguments;
import io.arconia.cli.openrewrite.recipes.ArconiaRecipe;
import io.arconia.cli.openrewrite.recipes.GradleRecipe;
import io.arconia.cli.openrewrite.recipes.MavenRecipe;
import io.arconia.cli.openrewrite.recipes.SpringAiRecipe;
import io.arconia.cli.openrewrite.recipes.SpringBootRecipe;

@Component
@Command(
    name = "update",
    aliases = {"upgrade"},
    description = "Update project dependencies and build tool versions."
)
public class UpdateCommands implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "gradle", description = "Update a Java project to the latest Gradle version.")
    public void gradle(
        @Option(names = "--dry-run", description = "Update in dry-run mode.") boolean dryRun,
        @Mixin OutputOptions outputOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(outputOptions, parametersOption.getParams());
        var updateOptions = UpdateArguments.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(GradleRecipe.RECIPE_NAME)
            .rewriteRecipeLibrary(GradleRecipe.RECIPE_LIBRARY)
            .build();

        openRewriteRunner.update(updateOptions);
    }

    @Command(name = "maven", description = "Update a Java project to the latest Maven version.")
    public void maven(
        @Option(names = "--dry-run", description = "Update in dry-run mode.") boolean dryRun,
        @Mixin OutputOptions outputOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(outputOptions, parametersOption.getParams());
        var updateOptions = UpdateArguments.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(MavenRecipe.RECIPE_NAME)
            .rewriteRecipeLibrary(MavenRecipe.RECIPE_LIBRARY)
            .build();

        openRewriteRunner.update(updateOptions);
    }

    @Command(name = "framework", description = "Update a Java project to a new Arconia Framework version.")
    public void framework(
        @Option(names = "--dry-run", description = "Update in dry-run mode.") boolean dryRun,
        @Option(names = "--to-version", defaultValue = "0.24", description = "Arconia target version.") String toVersion,
        @Mixin OutputOptions outputOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(outputOptions, parametersOption.getParams());
        var updateOptions = UpdateArguments.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(ArconiaRecipe.computeRecipeLibrary(toVersion))
            .rewriteRecipeLibrary(ArconiaRecipe.RECIPE_LIBRARY)
            .build();

        openRewriteRunner.update(updateOptions);
    }

    @Command(name = "spring-ai", description = "Update a Java project to a new Spring AI version.")
    public void springAi(
        @Option(names = "--dry-run", description = "Update in dry-run mode.") boolean dryRun,
        @Option(names = "--to-version", defaultValue = "1.1", description = "Spring AI target version.") String toVersion,
        @Mixin OutputOptions outputOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(outputOptions, parametersOption.getParams());
        var updateOptions = UpdateArguments.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(SpringAiRecipe.computeRecipeLibrary(toVersion))
            .rewriteRecipeLibrary(SpringAiRecipe.RECIPE_LIBRARY)
            .build();

        openRewriteRunner.update(updateOptions);
    }

    @Command(name = "spring-boot", description = "Update a Java project to a new Spring Boot version.")
    public void springBoot(
        @Option(names = "--dry-run", description = "Update in dry-run mode.") boolean dryRun,
        @Option(names = "--to-version", defaultValue = "4.0", description = "Spring Boot target version.") String toVersion,
        @Mixin OutputOptions outputOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(outputOptions, parametersOption.getParams());
        var updateOptions = UpdateArguments.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(SpringBootRecipe.computeRecipeLibrary(toVersion))
            .rewriteRecipeLibrary(SpringBootRecipe.RECIPE_LIBRARY)
            .build();

        openRewriteRunner.update(updateOptions);
    }

}
