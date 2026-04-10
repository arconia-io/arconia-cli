package io.arconia.cli.project.catalog.service;

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
 * Unit tests for {@link ProjectCatalogService}.
 */
class ProjectCatalogServiceTests {

    private ProjectCatalogService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ProjectCatalogService(Registry.builder().build(), createOutputOptions());
    }

    @Test
    void whenRegistryIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCatalogService(null, createOutputOptions()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registry cannot be null");
    }

    @Test
    void whenOutputOptionsIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectCatalogService(Registry.builder().build(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outputOptions cannot be null");
    }

    @Test
    void addCatalogThrowsWhenCatalogNameIsNull() {
        assertThatThrownBy(() -> service.addCatalog(null, "ghcr.io/org/catalogs/spring-templates:1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void addCatalogThrowsWhenCatalogNameIsEmpty() {
        assertThatThrownBy(() -> service.addCatalog("", "ghcr.io/org/catalogs/spring-templates:1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void addCatalogThrowsWhenCatalogRefIsNull() {
        assertThatThrownBy(() -> service.addCatalog("spring-templates", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogRef cannot be null or empty");
    }

    @Test
    void addCatalogThrowsWhenCatalogRefIsEmpty() {
        assertThatThrownBy(() -> service.addCatalog("spring-templates", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogRef cannot be null or empty");
    }

    @Test
    void removeCatalogThrowsWhenCatalogNameIsNull() {
        assertThatThrownBy(() -> service.removeCatalog(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void removeCatalogThrowsWhenCatalogNameIsEmpty() {
        assertThatThrownBy(() -> service.removeCatalog(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void updateCatalogThrowsWhenCatalogNameIsNull() {
        assertThatThrownBy(() -> service.updateCatalog(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void updateCatalogThrowsWhenCatalogNameIsEmpty() {
        assertThatThrownBy(() -> service.updateCatalog(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void listCatalogThrowsWhenCatalogNameIsNull() {
        assertThatThrownBy(() -> service.listCatalog(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
    }

    @Test
    void listCatalogThrowsWhenCatalogNameIsEmpty() {
        assertThatThrownBy(() -> service.listCatalog(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogName cannot be null or empty");
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
