package io.arconia.cli.commands;

import java.util.Arrays;
import java.util.List;

import io.arconia.cli.build.BuildOptions;
import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.core.ArconiaCliTerminal;

import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandRegistration.OptionArity;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(group = "Development")
public class DevelopmentCommands {

    @Command(command = "build", description = "Build the current project.")
    public void build(
        CommandContext commandContext,
        @Option(description = "Perform a clean build.") boolean clean,
        @Option(description = "Skip tests.") boolean skipTests,
        @Option(description = "Perform a native build.", longNames = "native") boolean nativeBuild,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(required = false, description = "Additional build parameters.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var buildToolRunner = BuildToolRunner.create(terminal);
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
        CommandContext commandContext,
        @Option(description = "Perform a clean build.") boolean clean,
        @Option(description = "Run tests in native mode", longNames = "native") boolean nativeBuild,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(required = false, description = "Additional build parameters.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var buildToolRunner = BuildToolRunner.create(terminal);
        var buildOptions = BuildOptions.builder()
            .clean(clean)
            .nativeBuild(nativeBuild)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        buildToolRunner.test(buildOptions);
    }

    @Command(command = "dev", alias = "run", description = "Run the application in development mode.")
    public void dev(
        CommandContext commandContext,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(required = false, description = "Additional build parameters.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var buildToolRunner = BuildToolRunner.create(terminal);
        var buildOptions = BuildOptions.builder()
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        buildToolRunner.dev(buildOptions);
    }

}
