package io.arconia.cli.core;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

public final class CliExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
        cmd.getErr().println(cmd.getColorScheme().errorText("❗ " + message));
        if (parseResult.hasMatchedOption("--verbose")) {
            cmd.getErr().println(cmd.getColorScheme().stackTraceText(ex));
        }
        cmd.getErr().flush();
        return cmd.getCommandSpec().exitCodeOnExecutionException();
    }

}
