package io.arconia.cli.commands;

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
    void helpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("rewrite", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia rewrite");
    }

}
