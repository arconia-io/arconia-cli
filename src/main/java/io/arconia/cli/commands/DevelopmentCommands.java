package io.arconia.cli.commands;

import java.util.Arrays;
import java.util.List;

import io.arconia.cli.build.BuildOptions;
import io.arconia.cli.build.BuildOptions.Mode;
import io.arconia.cli.build.BuildOptions.Trait;
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
        @Option(description = "Include more verbose output about the execution.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(description = "Additional build parameters passed directly to the build tool.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var buildToolRunner = BuildToolRunner.create(terminal);
        var buildOptions = BuildOptions.builder()
            .clean(clean)
            .skipTests(skipTests)
            .trait(nativeBuild ? Trait.NATIVE_BUILD : Trait.NONE)
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
        @Option(description = "Include more verbose output about the execution.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(description = "Additional build parameters passed directly to the build tool.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var buildToolRunner = BuildToolRunner.create(terminal);
        var buildOptions = BuildOptions.builder()
            .clean(clean)
            .mode(Mode.TEST)
            .trait(nativeBuild ? Trait.NATIVE_BUILD : Trait.NONE)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        buildToolRunner.test(buildOptions);
    }

    @Command(command = "dev", alias = "run", description = "Run the application in development mode.")
    public void dev(
        CommandContext commandContext,
        @Option(description = "Run from the test classpath.", shortNames = 't') boolean test,
        @Option(description = "Include debug output.", shortNames = 'd') boolean debug,
        @Option(description = "Include more verbose output about the execution.", shortNames = 'v') boolean verbose,
        @Option(description = "Include more details about errors.", shortNames = 's') boolean stacktrace,
        @Option(description = "Additional build parameters passed directly to the build tool.", shortNames = 'p', arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var terminal = new ArconiaCliTerminal(commandContext);
        var buildToolRunner = BuildToolRunner.create(terminal);
        var buildOptions = BuildOptions.builder()
            .mode(Mode.DEV)
            .trait(test ? Trait.TEST_CLASSPATH : Trait.NONE)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();

        buildToolRunner.dev(buildOptions);
    }

}
