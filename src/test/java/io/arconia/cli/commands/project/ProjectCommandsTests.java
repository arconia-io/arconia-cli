package io.arconia.cli.commands.project;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProjectCommands}.
 */
@SpringBootTest
class ProjectCommandsTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void helpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia project");
        assertThat(out).contains("create");
        assertThat(out).contains("push");
        assertThat(out).contains("collection");
    }

    @Test
    void projectCommandShowsUsage() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia project");
    }

    @Test
    void createHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project", "create", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia project create");
        assertThat(out).contains("--name");
        assertThat(out).contains("--template");
        assertThat(out).contains("--group");
        assertThat(out).contains("--description");
        assertThat(out).contains("--package-name");
        assertThat(out).contains("--path");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void pushHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project", "push", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia project push");
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

        int exitCode = commandLine.execute("project", "push");

        assertThat(exitCode).isNotZero();
        assertThat(error.toString()).contains("--ref");
    }

    @Test
    void pushRejectsRefAndBaseRefTogether() {
        var output = new StringWriter();
        var error = new StringWriter();
        commandLine.setOut(new PrintWriter(output));
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("project", "push", "--ref", "ghcr.io/org/projects/my-project", "--base-ref", "ghcr.io/org/projects");

        assertThat(exitCode).isNotZero();
        assertThat(error.toString()).contains("--ref");
    }

}
