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
    name = "dev",
    description = "Run the application in dev mode."
)
public class DevCommand implements Runnable {

    @Option(names = "--clean", description = "Perform a clean build.")
    boolean clean;

    @Option(names = "--offline", description = "Perform the build offline.")
    boolean offline;

    @Option(names = {"-t", "--test"}, description = "Run from the test classpath.")
    boolean test;

    @Mixin
    OutputOptions outputOptions;

    @Mixin
    ParametersOption parametersOption;

    @Override
    public void run() {
        var buildToolRunner = BuildToolRunner.create(outputOptions, parametersOption.getParams());
        var buildOptions = BuildArguments.builder()
            .clean(clean)
            .offline(offline)
            .testClasspath(test)
            .build();
        buildToolRunner.dev(buildOptions);
    }

}
