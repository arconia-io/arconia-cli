package io.arconia.cli.project.catalog;

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
 * Unit tests for {@link ProjectCatalogPublisher}.
 */
class ProjectCatalogPublisherTests {

    private ProjectCatalogPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ProjectCatalogPublisher(Registry.builder().build(), createOutputOptions());
    }

    @Test
    void whenRegistryIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCatalogPublisher(null, createOutputOptions()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registry cannot be null");
    }

    @Test
    void whenOutputOptionsIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCatalogPublisher(Registry.builder().build(), null))
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
        ProjectCatalogPushArguments args = ProjectCatalogPushArguments.builder()
                .ref("ghcr.io/org/catalogs/spring-templates")
                .tag("1.0.0")
                .name("spring-templates")
                .description("A test catalog")
                .annotations(Map.of())
                .build();

        assertThatThrownBy(() -> publisher.publish(args))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("No templates specified");
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
