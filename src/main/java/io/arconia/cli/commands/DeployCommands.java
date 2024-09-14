package io.arconia.cli.commands;

import org.springframework.shell.command.annotation.Command;

import io.arconia.cli.core.ArconiaTerminal;

@Command(command = "deploy", group = "Deployment", hidden = true)
public class DeployCommands {

    private final ArconiaTerminal terminal;

    public DeployCommands(ArconiaTerminal terminal) {
        this.terminal = terminal;
    }

    @Command(command = "kubernetes", description = "Deploy the application on Kubernetes.")
    public void kubernetes() {
        terminal.println("Under construction");
    }

    @Command(command = "knative", description = "Deploy the application on Knative.")
    public void knative() {
        terminal.println("Under construction");
    }

}
