package io.arconia.cli.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ImageBuildCommand}.
 */
@SpringBootTest
class ImageBuildCommandTests {

    @Autowired
    CommandLine commandLine;

    @Test
    void helpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("image", "build", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia image build");
    }

    @Test
    void buildpacksHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("image", "build", "buildpacks", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia image build buildpacks");
    }

    @Test
    void dockerfileHelpOption() {
        var output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("image", "build", "dockerfile", "--help");

        assertThat(exitCode).isZero();
        assertThat(output.toString()).contains("Usage: arconia image build dockerfile");
    }

}
