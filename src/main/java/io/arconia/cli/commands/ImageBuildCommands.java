package io.arconia.cli.commands;

import org.springframework.shell.command.annotation.Command;

import io.arconia.cli.core.ArconiaTerminal;

@Command(command = "image build", group = "Image", hidden = true)
public class ImageBuildCommands {

    private final ArconiaTerminal terminal;

    public ImageBuildCommands(ArconiaTerminal terminal) {
        this.terminal = terminal;
    }

    @Command(command = "buildpacks", description = "Build a container image using Buildpacks.")
    public void buildpacks() {
        terminal.println("Under construction");
    }

    @Command(command = "dockerfile", description = "Build a container image using Dockerfile.")
    public void dockerfile() {
        terminal.println("Under construction");
    }

}
