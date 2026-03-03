package io.arconia.cli.commands;

import org.springframework.stereotype.Component;

import io.arconia.cli.build.BuildOptions;
import io.arconia.cli.build.BuildOptions.Mode;
import io.arconia.cli.build.BuildOptions.Trait;
import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.core.ArconiaCliTerminal;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Component
@Command(
    name = "test",
    description = "Run tests for the current project."
)
public class TestCommand implements Runnable {

    @Option(names = "--clean", description = "Perform a clean build.")
    boolean clean;

    @Option(names = "--native", description = "Run tests in native mode.")
    boolean nativeBuild;

    @Mixin
    TroubleshootOptions troubleshootOptions;

    @Mixin
    ParametersOption parametersOption;

    private final ArconiaCliTerminal terminal;

    public TestCommand(ArconiaCliTerminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void run() {
        var buildToolRunner = BuildToolRunner.create(terminal, troubleshootOptions);
        var buildOptions = BuildOptions.builder()
            .clean(clean)
            .mode(Mode.TEST)
            .trait(nativeBuild ? Trait.NATIVE_BUILD : Trait.NONE)
            .params(parametersOption.getParams())
            .build();

        buildToolRunner.test(buildOptions);
    }

}
