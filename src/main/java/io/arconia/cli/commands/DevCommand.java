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
    name = "dev",
    aliases = {"run"},
    description = "Run the application in development mode."
)
public class DevCommand implements Runnable {

    @Option(names = {"-t", "--test"}, description = "Run from the test classpath.")
    boolean test;

    @Mixin
    TroubleshootOptions troubleshootOptions;

    @Mixin
    ParametersOption parametersOption;

    private final ArconiaCliTerminal terminal;

    public DevCommand(ArconiaCliTerminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void run() {
        var buildToolRunner = BuildToolRunner.create(terminal, troubleshootOptions);
        var buildOptions = BuildOptions.builder()
            .mode(Mode.DEV)
            .trait(test ? Trait.TEST_CLASSPATH : Trait.NONE)
            .params(parametersOption.getParams())
            .build();

        buildToolRunner.dev(buildOptions);
    }

}
