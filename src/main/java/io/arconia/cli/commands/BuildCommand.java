package io.arconia.cli.commands;

import org.springframework.stereotype.Component;

import io.arconia.cli.build.BuildOptions;
import io.arconia.cli.build.BuildOptions.Trait;
import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.core.ArconiaCliTerminal;
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

    @Mixin
    TroubleshootOptions troubleshootOptions;

    @Mixin
    ParametersOption parametersOption;

    private final ArconiaCliTerminal terminal;

    public BuildCommand(ArconiaCliTerminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void run() {
        var buildToolRunner = BuildToolRunner.create(terminal, troubleshootOptions);
        var buildOptions = BuildOptions.builder()
            .clean(clean)
            .skipTests(skipTests)
            .trait(nativeBuild ? Trait.NATIVE_BUILD : Trait.NONE)
            .params(parametersOption.getParams())
            .build();

        buildToolRunner.build(buildOptions);
    }

}
