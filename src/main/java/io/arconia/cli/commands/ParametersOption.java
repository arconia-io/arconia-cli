package io.arconia.cli.commands;

import java.util.List;

import picocli.CommandLine.Parameters;

public class ParametersOption {

    @Parameters(index = "0..*",
                description = "Additional parameters passed directly to the underlying tool.",
                paramLabel = "<param>",
                hidden = true)
    List<String> params;

    public List<String> getParams() {
        return params != null ? params : List.of();
    }

}
