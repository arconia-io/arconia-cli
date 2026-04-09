package io.arconia.cli.artifact;

import land.oras.Registry;

import io.arconia.cli.commands.options.RegistryOptions;

/**
 * Central factory for creating ORAS registry clients.
 */
public final class ArtifactRegistry {

    private ArtifactRegistry() {}

    /**
     * Creates an ORAS registry client with default settings.
     */
    public static Registry create() {
        return Registry.builder().defaults().build();
    }

    /**
     * Creates an ORAS registry client with the given options.
     */
    public static Registry create(RegistryOptions options) {
        return Registry.builder().defaults()
                .withInsecure(options.registryInsecure())
                .withSkipTlsVerify(options.registrySkipTlsVerify())
                .build();
    }

}
