package io.arconia.cli.commands;

import org.springframework.stereotype.Component;

import io.arconia.cli.commands.development.BuildCommand;
import io.arconia.cli.commands.development.DevCommand;
import io.arconia.cli.commands.development.TestCommand;
import io.arconia.cli.commands.image.ImageCommands;
import io.arconia.cli.commands.refactoring.RewriteCommand;
import io.arconia.cli.commands.refactoring.UpdateCommands;
import io.arconia.cli.core.CliVersionProvider;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Component
@Command(
    name = "arconia",
    commandListHeading = "%nCommands:%n",
    headerHeading = "%n",
    footer = "",
    mixinStandardHelpOptions = true,
    optionListHeading = "Options:%n",
    scope = ScopeType.INHERIT,
    showEndOfOptionsDelimiterInUsageHelp = true,
    sortOptions = false,
    versionProvider = CliVersionProvider.class,
    subcommands = {
        DevCommand.class,
        BuildCommand.class,
        TestCommand.class,
        ImageCommands.class,
        UpdateCommands.class,
        RewriteCommand.class,
        HelpCommand.class,
        VersionCommand.class
    }
)
public class ArconiaCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

}
