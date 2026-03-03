package io.arconia.cli.commands;

import org.springframework.stereotype.Component;

import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.openrewrite.OpenRewriteRunner;
import io.arconia.cli.openrewrite.UpdateOptions;
import io.arconia.cli.openrewrite.recipes.ArconiaRecipe;
import io.arconia.cli.openrewrite.recipes.GradleRecipe;
import io.arconia.cli.openrewrite.recipes.MavenRecipe;
import io.arconia.cli.openrewrite.recipes.SpringAiRecipe;
import io.arconia.cli.openrewrite.recipes.SpringBootRecipe;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Component
@Command(
    name = "update",
    aliases = {"upgrade"},
    description = "Update project dependencies and build tool versions."
)
public class UpdateCommands implements Runnable {

    @Spec
    CommandSpec spec;

    private final ArconiaCliTerminal terminal;

    public UpdateCommands(ArconiaCliTerminal terminal) {
        this.terminal = terminal;
    }
    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "gradle", description = "Update project to new Gradle version.")
    public void gradle(
        @Option(names = "--dry-run", description = "Update in dry-run mode.") boolean dryRun,
        @Mixin TroubleshootOptions troubleshootOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(terminal, troubleshootOptions);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(GradleRecipe.RECIPE_NAME)
            .rewriteRecipeLibrary(GradleRecipe.RECIPE_LIBRARY)
            .params(parametersOption.getParams())
            .build();

        openRewriteRunner.update(updateOptions);
    }

    @Command(name = "maven", description = "Update project to new Maven version.")
    public void maven(
        @Option(names = "--dry-run", description = "Update in dry-run mode.") boolean dryRun,
        @Mixin TroubleshootOptions troubleshootOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(terminal, troubleshootOptions);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(MavenRecipe.RECIPE_NAME)
            .rewriteRecipeLibrary(MavenRecipe.RECIPE_LIBRARY)
            .params(parametersOption.getParams())
            .build();

        openRewriteRunner.update(updateOptions);
    }

    @Command(name = "framework", description = "Update project to new Arconia Framework version.")
    public void framework(
        @Option(names = "--dry-run", description = "Update in dry-run mode.") boolean dryRun,
        @Option(names = "--to-version", defaultValue = "0.20", description = "Arconia target version.") String toVersion,
        @Mixin TroubleshootOptions troubleshootOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(terminal, troubleshootOptions);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(ArconiaRecipe.computeRecipeLibrary(toVersion))
            .rewriteRecipeLibrary(ArconiaRecipe.RECIPE_LIBRARY)
            .params(parametersOption.getParams())
            .build();

        openRewriteRunner.update(updateOptions);
    }

    @Command(name = "spring-ai", description = "Update project to new Spring AI version.")
    public void springAi(
        @Option(names = "--dry-run", description = "Update in dry-run mode.") boolean dryRun,
        @Option(names = "--to-version", defaultValue = "1.1", description = "Spring AI target version.") String toVersion,
        @Mixin TroubleshootOptions troubleshootOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(terminal, troubleshootOptions);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(SpringAiRecipe.computeRecipeLibrary(toVersion))
            .rewriteRecipeLibrary(SpringAiRecipe.RECIPE_LIBRARY)
            .params(parametersOption.getParams())
            .build();

        openRewriteRunner.update(updateOptions);
    }

    @Command(name = "spring-boot", description = "Update project to a new Spring Boot version.")
    public void springBoot(
        @Option(names = "--dry-run", description = "Update in dry-run mode.") boolean dryRun,
        @Option(names = "--to-version", defaultValue = "3.5", description = "Spring Boot target version.") String toVersion,
        @Mixin TroubleshootOptions troubleshootOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(terminal, troubleshootOptions);
        var updateOptions = UpdateOptions.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(SpringBootRecipe.computeRecipeLibrary(toVersion))
            .rewriteRecipeLibrary(SpringBootRecipe.RECIPE_LIBRARY)
            .params(parametersOption.getParams())
            .build();

        openRewriteRunner.update(updateOptions);
    }

}
