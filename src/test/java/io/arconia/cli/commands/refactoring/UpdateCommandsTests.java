package io.arconia.cli.commands.refactoring;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UpdateCommands}.
 */
@SpringBootTest
class UpdateCommandsTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void helpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("update", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia update");
    }

    @Test
    void gradleHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("update", "gradle", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia update gradle");
    }

    @Test
    void mavenHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("update", "maven", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia update maven");
    }

    @Test
    void frameworkHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("update", "framework", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia update framework");
    }

    @Test
    void springAiHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("update", "spring-ai", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia update spring-ai");
    }

    @Test
    void springBootHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("update", "spring-boot", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia update spring-boot");
    }

}
