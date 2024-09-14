package io.arconia.cli.commands;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import io.arconia.cli.core.ArconiaTerminal;
import io.arconia.cli.image.DockerfileRunner;

@Command(command = "image build", group = "Image")
public class ImageBuildCommands {

    private final ArconiaTerminal terminal;

    public ImageBuildCommands(ArconiaTerminal terminal) {
        this.terminal = terminal;
    }

    @Command(command = "buildpacks", description = "Build a container image using Buildpacks.", hidden = true)
    public void buildpacks() {
        terminal.println("Under construction");
    }

    @Command(command = "dockerfile", description = "Build a container image using Dockerfile.")
    public void dockerfile(
        @Option(description = "The full name of the container image to build.", shortNames = 't') String imageReference,
        @Option(required = false, description = "The path to the Dockerfile to use for building the container image.", shortNames = 'f') String dockerfile
    ) {
        var dockerfileRunner = new DockerfileRunner();
        dockerfileRunner.build(imageReference, dockerfile);
    }
    
}
