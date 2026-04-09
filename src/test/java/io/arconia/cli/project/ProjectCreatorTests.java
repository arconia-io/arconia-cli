package io.arconia.cli.project;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import land.oras.Registry;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import io.arconia.cli.commands.options.OutputOptions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCreator}.
 */
class ProjectCreatorTests {

    private ProjectCreator creator;

    @BeforeEach
    void setUp() {
        creator = new ProjectCreator(Registry.builder().build(), createOutputOptions());
    }

    @Test
    void whenRegistryIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCreator(null, createOutputOptions()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registry cannot be null");
    }

    @Test
    void whenOutputOptionsIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCreator(Registry.builder().build(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outputOptions cannot be null");
    }

    @Test
    void createThrowsWhenArgumentsIsNull() {
        assertThatThrownBy(() -> creator.create(null, "ghcr.io/org/templates/my-template:1.0.0", Path.of("/projects")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arguments cannot be null");
    }

    @Test
    void createThrowsWhenTemplateNameIsNull() {
        ProjectCreateArguments arguments = ProjectCreateArguments.builder()
                .name("my-app")
                .group("io.example")
                .build();

        assertThatThrownBy(() -> creator.create(arguments, null, Path.of("/projects")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateName cannot be null or empty");
    }

    @Test
    void createThrowsWhenTemplateNameIsEmpty() {
        ProjectCreateArguments arguments = ProjectCreateArguments.builder()
                .name("my-app")
                .group("io.example")
                .build();

        assertThatThrownBy(() -> creator.create(arguments, "", Path.of("/projects")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateName cannot be null or empty");
    }

    @Test
    void createThrowsWhenTargetParentDirectoryIsNull() {
        ProjectCreateArguments arguments = ProjectCreateArguments.builder()
                .name("my-app")
                .group("io.example")
                .build();

        assertThatThrownBy(() -> creator.create(arguments, "ghcr.io/org/templates/my-template:1.0.0", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetParentDirectory cannot be null");
    }

    private static OutputOptions createOutputOptions() {
        TestCommand testCommand = new TestCommand();
        new CommandLine(testCommand)
                .setOut(new PrintWriter(new StringWriter()))
                .setErr(new PrintWriter(new StringWriter()))
                .parseArgs();
        return testCommand.outputOptions;
    }

    @Command(name = "test")
    private static class TestCommand implements Runnable {
        @Mixin
        OutputOptions outputOptions = new OutputOptions();

        @Override
        public void run() {}
    }

}
