package io.arconia.cli.commands;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

import io.arconia.cli.commands.development.BuildCommand;
import io.arconia.cli.commands.development.CreateCommand;
import io.arconia.cli.commands.development.DevCommand;
import io.arconia.cli.commands.development.TestCommand;
import io.arconia.cli.commands.image.ImageCommands;
import io.arconia.cli.commands.refactoring.RewriteCommand;
import io.arconia.cli.commands.refactoring.UpdateCommands;
import io.arconia.cli.commands.skills.SkillsCommands;
import io.arconia.cli.commands.template.TemplateCommands;
import io.arconia.cli.core.CliVersionProvider;

@Component
@Command(
    name = "arconia",
    commandListHeading = "%nCommands:%n",
    headerHeading = "%n",
    footer = "",
    mixinStandardHelpOptions = true,
    optionListHeading = "Options:%n",
    scope = ScopeType.INHERIT,
    sortOptions = false,
    versionProvider = CliVersionProvider.class,
    subcommands = {
            CreateCommand.class,
            DevCommand.class,
            BuildCommand.class,
            TestCommand.class,
            ImageCommands.class,
            UpdateCommands.class,
            RewriteCommand.class,
            TemplateCommands.class,
            SkillsCommands.class,
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
