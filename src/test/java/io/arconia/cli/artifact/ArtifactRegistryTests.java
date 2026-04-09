package io.arconia.cli.artifact;

import org.junit.jupiter.api.Test;

import land.oras.Registry;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import io.arconia.cli.commands.options.RegistryOptions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArtifactRegistry}.
 */
class ArtifactRegistryTests {

    @Test
    void createDefaultRegistry() {
        Registry registry = ArtifactRegistry.create();

        assertThat(registry).isNotNull().isInstanceOf(Registry.class);
        assertThat(registry.isInsecure()).isFalse();
        assertThat(registry.getScheme()).isEqualTo("https");
    }

    @Test
    void createRegistryWithDefaultOptions() {
        var options = createRegistryOptions();

        Registry registry = ArtifactRegistry.create(options);

        assertThat(registry).isNotNull().isInstanceOf(Registry.class);
        assertThat(registry.isInsecure()).isFalse();
        assertThat(registry.getScheme()).isEqualTo("https");
    }

    @Test
    void createRegistryWithInsecure() {
        var options = createRegistryOptions("--registry-insecure");

        Registry registry = ArtifactRegistry.create(options);

        assertThat(registry).isNotNull();
        assertThat(registry.isInsecure()).isTrue();
        assertThat(registry.getScheme()).isEqualTo("http");
    }

    @Test
    void createRegistryWithSkipTlsVerify() {
        var options = createRegistryOptions("--registry-skip-tls-verify");

        Registry registry = ArtifactRegistry.create(options);

        assertThat(registry).isNotNull();
        assertThat(registry.isInsecure()).isFalse();
        assertThat(registry.getScheme()).isEqualTo("https");
    }

    @Test
    void createRegistryWithInsecureAndSkipTlsVerify() {
        var options = createRegistryOptions("--registry-insecure", "--registry-skip-tls-verify");

        Registry registry = ArtifactRegistry.create(options);

        assertThat(registry).isNotNull();
        assertThat(registry.isInsecure()).isTrue();
        assertThat(registry.getScheme()).isEqualTo("http");
    }

    private RegistryOptions createRegistryOptions(String... args) {
        var options = new RegistryOptions();
        var spec = CommandSpec.create()
            .name("test-cmd")
            .addMixin("registryOptions", CommandSpec.forAnnotatedObject(options));
        new CommandLine(spec).parseArgs(args);
        return options;
    }

}
