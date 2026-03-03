package io.arconia.cli.core;

import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

@Component
public class ArconiaExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
        cmd.getErr().println(cmd.getColorScheme().errorText("❗ " + ex.getMessage()));
        return cmd.getCommandSpec().exitCodeOnExecutionException();
    }

}
