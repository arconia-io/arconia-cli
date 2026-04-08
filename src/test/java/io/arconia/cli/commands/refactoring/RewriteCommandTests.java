package io.arconia.cli.commands.refactoring;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RewriteCommand}.
 */
@SpringBootTest
class RewriteCommandTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void rewriteHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("rewrite", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia rewrite");
        assertThat(out).contains("run");
        assertThat(out).contains("discover");
    }

    @Test
    void rewriteCommandShowsUsage() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("rewrite");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia rewrite");
    }

    @Test
    void runHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("rewrite", "run", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia rewrite run");
        assertThat(out).contains("--recipe-name");
        assertThat(out).contains("--recipe-library");
        assertThat(out).contains("--recipe-version");
        assertThat(out).contains("--dry-run");
    }

    @Test
    void runRequiresRecipeName() {
        var error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("rewrite", "run");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void discoverHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("rewrite", "discover", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia rewrite discover");
    }

}
