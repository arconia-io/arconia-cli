package io.arconia.cli.project;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import land.oras.Registry;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.project.oci.ProjectConfig;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProjectPublisher}.
 */
class ProjectPublisherTests {

    private ProjectPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ProjectPublisher(Registry.builder().build(), createOutputOptions());
    }

    @Test
    void whenRegistryIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectPublisher(null, createOutputOptions()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registry cannot be null");
    }

    @Test
    void whenOutputOptionsIsNullThenThrow() {
        assertThatThrownBy(() -> new ProjectPublisher(Registry.builder().build(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outputOptions cannot be null");
    }

    @Test
    void publishThrowsWhenArgumentsIsNull() {
        assertThatThrownBy(() -> publisher.publish((ProjectPushArguments) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arguments cannot be null");
    }

    @Test
    void publishWithConfigThrowsWhenArgumentsIsNull() {
        ProjectConfig config = ProjectConfig.builder()
                .name("my-app")
                .description("A test application")
                .build();

        assertThatThrownBy(() -> publisher.publish(null, config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arguments cannot be null");
    }

    @Test
    void publishWithConfigThrowsWhenConfigIsNull() {
        ProjectPushArguments args = ProjectPushArguments.builder()
                .ref("localhost:5000/test/my-app")
                .tag("1.0.0")
                .annotations(Map.of())
                .projectPath(Path.of("/projects/my-app"))
                .build();

        assertThatThrownBy(() -> publisher.publish(args, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectConfig cannot be null");
    }

    @Test
    void publishBatchThrowsWhenArgumentsIsNull() {
        assertThatThrownBy(() -> publisher.publish((ProjectBatchPushArguments) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arguments cannot be null");
    }

    private static OutputOptions createOutputOptions() {
        TestCommand testCommand = new TestCommand();
        new CommandLine(testCommand)
                .setOut(new PrintWriter(new StringWriter()))
                .setErr(new PrintWriter(new StringWriter()))
                .parseArgs();
        return testCommand.outputOptions;
    }

    @Command(name = "test")
    private static class TestCommand implements Runnable {
        @Mixin
        OutputOptions outputOptions = new OutputOptions();

        @Override
        public void run() {}
    }

}
