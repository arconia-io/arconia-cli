package io.arconia.cli.commands;

import org.springframework.stereotype.Component;

import io.arconia.cli.config.VersionProvider;
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
    mixinStandardHelpOptions = true,
    optionListHeading = "Options:%n",
    parameterListHeading = "%n",
    scope = ScopeType.INHERIT,
    sortOptions = false,
    versionProvider = VersionProvider.class,
    subcommands = {
        HelpCommand.class,
        DevCommand.class,
        BuildCommand.class,
        TestCommand.class,
        ImageCommands.class,
        UpdateCommands.class,
        RewriteCommand.class
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
