package io.arconia.cli.commands.development;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CreateCommand}.
 */
@SpringBootTest
class CreateCommandTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void helpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("create", "--help");

        assertThat(exitCode).isZero();
        String out = output.toString();
        assertThat(out).contains("Usage: arconia create");
        assertThat(out).contains("--name");
        assertThat(out).contains("--template");
        assertThat(out).contains("--group");
        assertThat(out).contains("--description");
        assertThat(out).contains("--package-name");
        assertThat(out).contains("--path");
        assertThat(out).contains("--registry-insecure");
        assertThat(out).contains("--registry-skip-tls-verify");
    }

}
