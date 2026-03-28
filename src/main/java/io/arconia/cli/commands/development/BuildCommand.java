package io.arconia.cli.commands.development;

import org.springframework.stereotype.Component;

import io.arconia.cli.build.BuildArguments;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.commands.options.ParametersOption;
import io.arconia.cli.build.BuildToolRunner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Component
@Command(
    name = "build",
    description = "Build the current project."
)
public class BuildCommand implements Runnable {

    @Option(names = "--clean", description = "Perform a clean build.")
    boolean clean;

    @Option(names = "--skip-tests", description = "Skip tests.")
    boolean skipTests;

    @Option(names = "--native", description = "Perform a native build.")
    boolean nativeBuild;

    @Option(names = "--offline", description = "Perform the build offline.")
    boolean offline;

    @Mixin
    OutputOptions outputOptions;

    @Mixin
    ParametersOption parametersOption;

    @Override
    public void run() {
        var buildToolRunner = BuildToolRunner.create(outputOptions, parametersOption.getParams());
        var buildOptions = BuildArguments.builder()
            .clean(clean)
            .skipTests(skipTests)
            .nativeBuild(nativeBuild)
            .offline(offline)
            .build();
        buildToolRunner.build(buildOptions);
    }

}
