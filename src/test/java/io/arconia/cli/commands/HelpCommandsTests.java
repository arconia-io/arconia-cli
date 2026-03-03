package io.arconia.cli.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HelpCommandsTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void helpCommand() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia");
    }

    @Test
    void helpOptionInTopCommand() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia");
    }

    @Test
    void helpOptionInBuildCommand() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("build", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia build");
    }

    @Test
    void helpOptionInUpdateCommand() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("update", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia update");
    }

    @Test
    void helpOptionInImageBuildCommand() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("image", "build", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia image build");
    }

}
