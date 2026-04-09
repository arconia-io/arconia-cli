package io.arconia.cli.commands.refactoring;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.commands.options.ParametersOption;
import io.arconia.cli.openrewrite.OpenRewriteRunner;
import io.arconia.cli.openrewrite.RewriteArguments;

@Component
@Command(
    name = "rewrite",
    description = "Discover and run OpenRewrite recipes to migrate or refactor a Java application."
)
public class RewriteCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "run", description = "Run an OpenRewrite recipe to migrate or refactor your Java application.")
    public void run(
        @Option(names = "--dry-run", description = "Run in dry-run mode.") boolean dryRun,
        @Option(names = "--recipe-name", required = true,
            description = "Name of the OpenRewrite recipe to run. Example: 'io.arconia.rewrite.spring.ai.UpgradeSpringAi_1_0'.") String recipeName,
        @Option(names = "--recipe-library",
            description = """
                Maven coordinates of the library containing the OpenRewrite recipe to run.
                If not provided, the OpenRewrite OSS and Arconia recipe libraries will be used.
                Example: 'io.arconia.migrations:rewrite-arconia'.""") String recipeLibrary,
        @Option(names = "--recipe-version",
            description = """
                Version of the library containing the OpenRewrite recipe to run.
                If not provided, the latest available version will be used. Example: '4.2.0'.""") String recipeVersion,
        @Mixin OutputOptions outputOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(outputOptions, parametersOption.getParams());
        var rewriteOptionsBuilder = RewriteArguments.builder()
            .dryRun(dryRun)
            .rewriteRecipeName(recipeName);

        if (StringUtils.hasText(recipeLibrary)) {
            rewriteOptionsBuilder.rewriteRecipeLibrary(recipeLibrary);
        }

        if (StringUtils.hasText(recipeVersion)) {
            rewriteOptionsBuilder.rewriteRecipeVersion(recipeVersion);
        }

        openRewriteRunner.rewriteRun(rewriteOptionsBuilder.build());
    }

    @Command(name = "discover", description = "Discover available OpenRewrite recipes.")
    public void discover(
        @Mixin OutputOptions outputOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var openRewriteRunner = new OpenRewriteRunner(outputOptions, parametersOption.getParams());
        openRewriteRunner.rewriteDiscover();
    }

}
