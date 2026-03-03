package io.arconia.cli.commands;

import picocli.CommandLine.Option;

public class TroubleshootOptions {

    @Option(names = {"-d", "--debug"}, description = "Include debug output.")
    boolean debug;

    @Option(names = {"-v", "--verbose"}, description = "Include more verbose output about the execution.")
    boolean verbose;

    @Option(names = {"-s", "--stacktrace"}, description = "Include more details about errors.")
    boolean stacktrace;

    public boolean isDebug() {
        return debug;
    }

    public boolean isVerbose() {
        return verbose || debug;
    }

    public boolean isStacktrace() {
        return stacktrace;
    }

}
