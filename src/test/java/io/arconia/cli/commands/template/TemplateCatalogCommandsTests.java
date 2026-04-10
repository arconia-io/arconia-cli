package io.arconia.cli.commands.template;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TemplateCatalogCommands}.
 */
@SpringBootTest
class TemplateCatalogCommandsTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void catalogHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("template", "catalog", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia template catalog");
        assertThat(out).contains("add");
        assertThat(out).contains("remove");
        assertThat(out).contains("update");
        assertThat(out).contains("push");
    }

    @Test
    void catalogCommandShowsUsage() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("template", "catalog");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia template catalog");
    }

    @Test
    void addHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("template", "catalog", "add", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia template catalog add");
        assertThat(out).contains("--name");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void addRequiresNameAndRef() {
        var error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("template", "catalog", "add");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void removeHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("template", "catalog", "remove", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia template catalog remove");
        assertThat(out).contains("--name");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void removeRequiresName() {
        var error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("template", "catalog", "remove");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void updateHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("template", "catalog", "update", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia template catalog update");
        assertThat(out).contains("--name");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void pushHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("template", "catalog", "push", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia template catalog push");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--name");
        assertThat(out).contains("--description");
        assertThat(out).contains("--tag");
        assertThat(out).contains("--from-report");
        assertThat(out).contains("--template");
        assertThat(out).contains("--annotation");
        assertThat(out).contains("--output-report");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void pushRequiresRefAndName() {
        var error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("template", "catalog", "push");

        assertThat(exitCode).isNotZero();
    }

}
