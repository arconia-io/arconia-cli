package io.arconia.cli.commands.skills;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SkillsCollectionCommands}.
 */
@SpringBootTest
class SkillsCollectionCommandsTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void collectionHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "collection", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills collection");
        assertThat(out).contains("push");
        assertThat(out).contains("list");
        assertThat(out).contains("add");
        assertThat(out).contains("remove");
        assertThat(out).contains("update");
    }

    @Test
    void collectionCommandShowsUsage() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "collection");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia skills collection");
    }

    @Test
    void collectionPushHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "collection", "push", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills collection push");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--name");
        assertThat(out).contains("--tag");
        assertThat(out).contains("--from-report");
        assertThat(out).contains("--skill");
        assertThat(out).contains("--additional-tag");
        assertThat(out).contains("--description");
        assertThat(out).contains("--annotation");
        assertThat(out).contains("--output-report");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void collectionPushRequiresRefAndName() {
        var output = new StringWriter();
        var error = new StringWriter();
        commandLine.setOut(new PrintWriter(output));
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("skills", "collection", "push");

        assertThat(exitCode).isNotZero();
        assertThat(error.toString()).contains("--ref");
    }

    @Test
    void collectionListHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "collection", "list", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills collection list");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--name");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void collectionAddHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "collection", "add", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills collection add");
        assertThat(out).contains("--name");
        assertThat(out).contains("--ref");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void collectionAddRequiresNameAndRef() {
        var error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("skills", "collection", "add");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void collectionRemoveHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "collection", "remove", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills collection remove");
        assertThat(out).contains("--name");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

    @Test
    void collectionRemoveRequiresName() {
        var error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("skills", "collection", "remove");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void collectionUpdateHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("skills", "collection", "update", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia skills collection update");
        assertThat(out).contains("--name");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

}
