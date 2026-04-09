package io.arconia.cli.commands.skills;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SkillsCommands}.
 */
@SpringBootTest
class SkillsCommandsTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void helpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills");
        assertThat(out).contains("add");
        assertThat(out).contains("install");
        assertThat(out).contains("list");
        assertThat(out).contains("remove");
        assertThat(out).contains("update");
        assertThat(out).contains("push");
        assertThat(out).contains("collection");
    }

    @Test
    void skillsCommandShowsUsage() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia skills");
    }

    @Test
    void addHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "add", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills add");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--name");
        assertThat(out).contains("--agent");
        assertThat(out).contains("--collection");
        assertThat(out).contains("--project-dir");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void installHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "install", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills install");
        assertThat(out).contains("--project-dir");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void listHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "list", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills list");
        assertThat(out).contains("--project-dir");
    }

    @Test
    void removeHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "remove", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills remove");
        assertThat(out).contains("--name");
        assertThat(out).contains("--yes");
        assertThat(out).contains("-y");
        assertThat(out).contains("--project-dir");
    }

    @Test
    void updateHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "update", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills update");
        assertThat(out).contains("--name");
        assertThat(out).contains("--all");
        assertThat(out).contains("--project-dir");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void pushHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "push", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills push");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--path");
        assertThat(out).contains("--tag");
        assertThat(out).contains("--all");
        assertThat(out).contains("--output-report");
        assertThat(out).contains("--additional-tag");
        assertThat(out).contains("--annotation");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void addRequiresRefOrNameOption() {
        var output = new StringWriter();
        var error = new StringWriter();
        commandLine.setOut(new PrintWriter(output));
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("skills", "add");

        assertThat(exitCode).isNotZero();
        assertThat(error.toString()).contains("--ref");
    }

    @Test
    void removeRequiresNameOption() {
        var output = new StringWriter();
        var error = new StringWriter();
        commandLine.setOut(new PrintWriter(output));
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("skills", "remove");

        assertThat(exitCode).isNotZero();
        assertThat(error.toString()).contains("--name");
    }

    @Test
    void pushRequiresRefAndPathOptions() {
        var output = new StringWriter();
        var error = new StringWriter();
        commandLine.setOut(new PrintWriter(output));
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("skills", "push");

        assertThat(exitCode).isNotZero();
        assertThat(error.toString()).contains("--ref");
    }

}
