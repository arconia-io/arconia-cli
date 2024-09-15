package io.arconia.cli.commands;

import java.util.Arrays;
import java.util.List;

import io.arconia.cli.build.BuildOptions;
import io.arconia.cli.build.BuildToolRunner;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(group = "Development")
public class DevelopmentCommands {

    @Command(command = "build", description = "Build the current project.")
    public void build(
        @Option(description = "Perform a clean build.") boolean clean,
        @Option(description = "Skip tests") boolean skipTests,
        @Option(description = "Perform a native build", longNames = "native") boolean nativeBuild,
        @Option(required = false, description = "Additional build parameters") String[] params
    ) {
        var buildToolRunner = BuildToolRunner.create();
        var buildOptions = BuildOptions.builder()
            .clean(clean)
            .skipTests(skipTests)
            .nativeBuild(nativeBuild)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();
        buildToolRunner.build(buildOptions);
    }

    @Command(command = "test", description = "Run tests for the current project.")
    public void test(
        @Option(description = "Perform a clean build.") boolean clean,
        @Option(description = "Run tests in native mode", longNames = "native") boolean nativeBuild,
        @Option(required = false, description = "Additional build parameters") String[] params
    ) {
        var buildToolRunner = BuildToolRunner.create();
        var buildOptions = BuildOptions.builder()
            .clean(clean)
            .nativeBuild(nativeBuild)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();
        buildToolRunner.test(buildOptions);
    }

    @Command(command = "run", description = "Run the application.")
    public void run(
        @Option(required = false, description = "Additional run parameters") String[] params
    ) {
        var buildToolRunner = BuildToolRunner.create();
        var buildOptions = BuildOptions.builder()
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();
        buildToolRunner.run(buildOptions);
    }

}
