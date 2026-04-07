package io.arconia.cli.commands.options;

import java.io.PrintWriter;

import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

public class OutputOptions {

    @Option(names = {"-v", "--verbose"}, description = "Include verbose output.")
    boolean verbose;

    @Spec(Spec.Target.MIXEE)
    CommandSpec mixee;

    public boolean isVerbose() {
        return verbose;
    }

    public PrintWriter out() {
        return mixee.commandLine().getOut();
    }

    public PrintWriter err() {
        return mixee.commandLine().getErr();
    }

    public ColorScheme colorScheme() {
        return mixee.commandLine().getColorScheme();
    }

    public void info(String... messages) {
        for (var message : messages) {
            out().println(message);
        }
        out().flush();
    }

    public void error(String message) {
        err().println(colorScheme().errorText("❗ %s".formatted(message)));
        err().flush();
    }

    public void verbose(String... messages) {
        if (verbose) {
            for (var message : messages) {
                out().println(colorScheme().ansi().text("@|faint 📝 %s|@".formatted(message)));
            }
            out().flush();
        }
    }

    public void table(String table) {
        out().print(table);
        out().flush();
    }

    public void newLine() {
        out().println();
        out().flush();
    }

}
