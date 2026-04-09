package io.arconia.cli.commands.development;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import io.arconia.cli.build.BuildArguments;
import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.commands.options.ParametersOption;

@Component
@Command(
    name = "test",
    description = "Run tests for a Java application."
)
public class TestCommand implements Runnable {

    @Option(names = "--clean", description = "Perform a clean build.")
    boolean clean;

    @Option(names = "--native", description = "Run tests in native mode.")
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
            .nativeBuild(nativeBuild)
            .offline(offline)
            .build();
        buildToolRunner.test(buildOptions);
    }

}
