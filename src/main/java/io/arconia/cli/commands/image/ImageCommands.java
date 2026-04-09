package io.arconia.cli.commands.image;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Component
@Command(
    name = "image",
    description = "Build and manage container images for Spring Boot applications.",
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
