package io.arconia.cli.commands.template;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TemplateCommands}.
 */
@SpringBootTest
class TemplateCommandsTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void helpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("template", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia template");
        assertThat(out).contains("list");
        assertThat(out).contains("push");
        assertThat(out).contains("catalog");
    }

    @Test
    void templateCommandShowsUsage() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("template");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia template");
    }

    @Test
    void listHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("template", "list", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia template list");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void pushHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("template", "push", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia template push");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--base-ref");
        assertThat(out).contains("--path");
        assertThat(out).contains("--tag");
        assertThat(out).contains("--annotation");
        assertThat(out).contains("--output-report");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void pushRequiresRefOrBaseRef() {
        var output = new StringWriter();
        var error = new StringWriter();
        commandLine.setOut(new PrintWriter(output));
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("template", "push");

        assertThat(exitCode).isNotZero();
        assertThat(error.toString()).contains("--ref");
    }

    @Test
    void pushRejectsRefAndBaseRefTogether() {
        var output = new StringWriter();
        var error = new StringWriter();
        commandLine.setOut(new PrintWriter(output));
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("template", "push", "--ref", "ghcr.io/org/projects/my-project", "--base-ref", "ghcr.io/org/projects");

        assertThat(exitCode).isNotZero();
        assertThat(error.toString()).contains("--ref");
    }

}
