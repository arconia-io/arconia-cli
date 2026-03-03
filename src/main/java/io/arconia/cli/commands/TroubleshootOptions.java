package io.arconia.cli.commands;

import picocli.CommandLine.Option;

public class TroubleshootOptions {

    @Option(names = {"-v", "--verbose"}, description = "Include verbose output.")
    boolean verbose;

    public boolean isVerbose() {
        return verbose;
    }

}
