package io.arconia.cli.image;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.Test;

import land.oras.Registry;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import io.arconia.cli.commands.options.OutputOptions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MultiArchImagePublisher}.
 */
class MultiArchImagePublisherTests {

    @Test
    void whenRegistryIsNullThenThrow() {
        assertThatThrownBy(() -> new MultiArchImagePublisher(null, createOutputOptions()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registry cannot be null");
    }

    @Test
    void whenOutputOptionsIsNullThenThrow() {
        assertThatThrownBy(() -> new MultiArchImagePublisher(Registry.builder().build(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outputOptions cannot be null");
    }

    @Test
    void publishWithNullBaseImageNameThrows() {
        var publisher = new MultiArchImagePublisher(Registry.builder().build(), createOutputOptions());
        var platformImages = List.of(new MultiArchImagePublisher.PlatformImage("reg/app:1.0-amd64", "linux/amd64"));
        assertThatThrownBy(() -> publisher.publish(null, platformImages))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publishWithEmptyPlatformImagesThrows() {
        var publisher = new MultiArchImagePublisher(Registry.builder().build(), createOutputOptions());
        assertThatThrownBy(() -> publisher.publish("reg/app:1.0", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void platformImageWithNullImageRefThrows() {
        assertThatThrownBy(() -> new MultiArchImagePublisher.PlatformImage(null, "linux/amd64"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void platformImageWithNullPlatformStringThrows() {
        assertThatThrownBy(() -> new MultiArchImagePublisher.PlatformImage("reg/app:1.0-amd64", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void platformImageWithEmptyImageRefThrows() {
        assertThatThrownBy(() -> new MultiArchImagePublisher.PlatformImage("", "linux/amd64"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void platformImageWithEmptyPlatformStringThrows() {
        assertThatThrownBy(() -> new MultiArchImagePublisher.PlatformImage("reg/app:1.0-amd64", ""))
                .isInstanceOf(IllegalArgumentException.class);
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
