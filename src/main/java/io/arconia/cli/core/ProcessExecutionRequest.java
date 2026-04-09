package io.arconia.cli.core;

import java.io.File;
import java.util.Map;

import org.springframework.util.Assert;

import io.arconia.cli.commands.options.OutputOptions;

/**
 * Represents a request to execute a process.
 */
public record ProcessExecutionRequest(
        String[] command,
        File targetDirectory,
        Map<String, String> environmentVariables,
        OutputOptions outputOptions
) {

    public ProcessExecutionRequest {
        Assert.notEmpty(command, "command cannot be null or empty");
        Assert.notNull(targetDirectory, "targetDirectory cannot be null");
        Assert.isTrue(targetDirectory.isDirectory(), "targetDirectory must be an existing directory");
        Assert.notNull(environmentVariables, "environmentVariables cannot be null");
        Assert.notNull(outputOptions, "outputOptions cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String[] command;
        private File targetDirectory;
        private Map<String, String> environmentVariables = Map.of();
        private OutputOptions outputOptions;

        private Builder() {}

        public Builder command(String[] command) {
            this.command = command;
            return this;
        }

        public Builder targetDirectory(File targetDirectory) {
            this.targetDirectory = targetDirectory;
            return this;
        }

        public Builder environmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables;
            return this;
        }

        public Builder outputOptions(OutputOptions outputOptions) {
            this.outputOptions = outputOptions;
            return this;
        }

        public ProcessExecutionRequest build() {
            return new ProcessExecutionRequest(command, targetDirectory, environmentVariables, outputOptions);
        }

    }

}
