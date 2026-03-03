package io.arconia.cli.commands;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Component
@Command(
    name = "version",
    description = "Display version information about the Arconia CLI."
)
public class VersionCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().printVersionHelp(spec.commandLine().getOut());
    }

}
