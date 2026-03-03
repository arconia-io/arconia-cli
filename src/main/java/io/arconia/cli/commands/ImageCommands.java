package io.arconia.cli.commands;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Component
@Command(
    name = "image",
    description = "Build and manage container images.",
    mixinStandardHelpOptions = true,
    subcommands = {
        ImageBuildCommands.class
    }
)
public class ImageCommands implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

}
