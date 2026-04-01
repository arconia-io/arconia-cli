package io.arconia.cli.commands.skills;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SkillsCatalogCommands}.
 */
@SpringBootTest
class SkillsCatalogCommandsTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void catalogHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "catalog", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills catalog");
        assertThat(out).contains("push");
        assertThat(out).contains("list");
        assertThat(out).contains("add");
        assertThat(out).contains("remove");
    }

    @Test
    void catalogCommandShowsUsage() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "catalog");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia skills catalog");
    }

    @Test
    void catalogPushHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "catalog", "push", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills catalog push");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--name");
        assertThat(out).contains("--tag");
        assertThat(out).contains("--from-report");
        assertThat(out).contains("--skill");
        assertThat(out).contains("--additional-tag");
        assertThat(out).contains("--description");
        assertThat(out).contains("--annotation");
    }

    @Test
    void catalogPushRequiresRefAndName() {
        var output = new StringWriter();
        var error = new StringWriter();
        commandLine.setOut(new PrintWriter(output));
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("skills", "catalog", "push");

        assertThat(exitCode).isNotZero();
        assertThat(error.toString()).contains("--ref");
    }

    @Test
    void catalogListHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "catalog", "list", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills catalog list");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--name");
    }

    @Test
    void catalogAddHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "catalog", "add", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills catalog add");
        assertThat(out).contains("--name");
        assertThat(out).contains("--ref");
    }

    @Test
    void catalogAddRequiresNameAndRef() {
        var error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("skills", "catalog", "add");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void catalogRemoveHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "catalog", "remove", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills catalog remove");
        assertThat(out).contains("--name");
    }

    @Test
    void catalogRemoveRequiresName() {
        var error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("skills", "catalog", "remove");

        assertThat(exitCode).isNotZero();
    }

}
