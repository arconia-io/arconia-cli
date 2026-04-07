package io.arconia.cli.project.collection;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import land.oras.Registry;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCollectionPublisher}.
 */
class ProjectCollectionPublisherTests {

    private ProjectCollectionPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ProjectCollectionPublisher(Registry.builder().build(), createOutputOptions());
    }

    @Test
    void whenRegistryIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCollectionPublisher(null, createOutputOptions()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registry cannot be null");
    }

    @Test
    void whenOutputOptionsIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCollectionPublisher(Registry.builder().build(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outputOptions cannot be null");
    }

    @Test
    void publishThrowsWhenArgumentsIsNull() {
        assertThatThrownBy(() -> publisher.publish(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arguments cannot be null");
    }

    @Test
    void publishThrowsWhenNeitherProjectsNorFromReportIsSpecified() {
        ProjectCollectionPushArguments args = ProjectCollectionPushArguments.builder()
                .ref("ghcr.io/org/collections/spring-templates")
                .tag("1.0.0")
                .name("spring-templates")
                .description("A test collection")
                .annotations(Map.of())
                .build();

        assertThatThrownBy(() -> publisher.publish(args))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("No projects specified");
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
