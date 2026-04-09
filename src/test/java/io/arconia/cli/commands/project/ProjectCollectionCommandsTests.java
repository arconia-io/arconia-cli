package io.arconia.cli.commands.project;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProjectCollectionCommands}.
 */
@SpringBootTest
class ProjectCollectionCommandsTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void collectionHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project", "collection", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia project collection");
        assertThat(out).contains("add");
        assertThat(out).contains("remove");
        assertThat(out).contains("update");
        assertThat(out).contains("list");
        assertThat(out).contains("push");
    }

    @Test
    void collectionCommandShowsUsage() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project", "collection");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia project collection");
    }

    @Test
    void addHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project", "collection", "add", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia project collection add");
        assertThat(out).contains("--name");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void addRequiresNameAndRef() {
        var error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("project", "collection", "add");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void removeHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project", "collection", "remove", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia project collection remove");
        assertThat(out).contains("--name");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void removeRequiresName() {
        var error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("project", "collection", "remove");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void listHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project", "collection", "list", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia project collection list");
        assertThat(out).contains("--name");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void updateHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project", "collection", "update", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia project collection update");
        assertThat(out).contains("--name");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void pushHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project", "collection", "push", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia project collection push");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--name");
        assertThat(out).contains("--description");
        assertThat(out).contains("--tag");
        assertThat(out).contains("--from-report");
        assertThat(out).contains("--project");
        assertThat(out).contains("--annotation");
        assertThat(out).contains("--report");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void pushRequiresRefAndName() {
        var error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("project", "collection", "push");

        assertThat(exitCode).isNotZero();
    }

}
