package io.arconia.cli.commands;

import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.annotation.Command;

import io.arconia.cli.core.ArconiaCliTerminal;

@Command(command = "deploy", group = "Deployment", hidden = true)
public class DeployCommands {

    @Command(command = "kubernetes", description = "Deploy the application on Kubernetes.")
    public void kubernetes(CommandContext commandContext) {
        var terminal = new ArconiaCliTerminal(commandContext);
        terminal.warn("Under construction");
    }

    @Command(command = "knative", description = "Deploy the application on Knative.")
    public void knative(CommandContext commandContext) {
        var terminal = new ArconiaCliTerminal(commandContext);
        terminal.warn("Under construction");
    }

}
