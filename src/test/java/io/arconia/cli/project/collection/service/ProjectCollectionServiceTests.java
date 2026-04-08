package io.arconia.cli.project.collection.service;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import land.oras.Registry;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import io.arconia.cli.commands.options.OutputOptions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectCollectionService}.
 */
class ProjectCollectionServiceTests {

    private ProjectCollectionService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ProjectCollectionService(Registry.builder().build(), createOutputOptions());
    }

    @Test
    void whenRegistryIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCollectionService(null, createOutputOptions()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registry cannot be null");
    }

    @Test
    void whenOutputOptionsIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCollectionService(Registry.builder().build(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outputOptions cannot be null");
    }

    @Test
    void addCollectionThrowsWhenCollectionNameIsNull() {
        assertThatThrownBy(() -> service.addCollection(null, "ghcr.io/org/collections/spring-templates:1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void addCollectionThrowsWhenCollectionNameIsEmpty() {
        assertThatThrownBy(() -> service.addCollection("", "ghcr.io/org/collections/spring-templates:1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void addCollectionThrowsWhenCollectionRefIsNull() {
        assertThatThrownBy(() -> service.addCollection("spring-templates", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionRef cannot be null or empty");
    }

    @Test
    void addCollectionThrowsWhenCollectionRefIsEmpty() {
        assertThatThrownBy(() -> service.addCollection("spring-templates", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionRef cannot be null or empty");
    }

    @Test
    void removeCollectionThrowsWhenCollectionNameIsNull() {
        assertThatThrownBy(() -> service.removeCollection(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void removeCollectionThrowsWhenCollectionNameIsEmpty() {
        assertThatThrownBy(() -> service.removeCollection(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void updateCollectionThrowsWhenCollectionNameIsNull() {
        assertThatThrownBy(() -> service.updateCollection(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void updateCollectionThrowsWhenCollectionNameIsEmpty() {
        assertThatThrownBy(() -> service.updateCollection(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void listCollectionThrowsWhenCollectionNameIsNull() {
        assertThatThrownBy(() -> service.listCollection(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
    }

    @Test
    void listCollectionThrowsWhenCollectionNameIsEmpty() {
        assertThatThrownBy(() -> service.listCollection(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName cannot be null or empty");
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
