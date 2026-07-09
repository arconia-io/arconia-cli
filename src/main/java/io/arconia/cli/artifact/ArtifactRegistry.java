package io.arconia.cli.artifact;

import land.oras.Registry;

import io.arconia.cli.commands.options.RegistryOptions;
import land.oras.policy.ContainersPolicy;

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
                .withPolicy(ContainersPolicy.newPolicy())
                .withSkipTlsVerify(options.registrySkipTlsVerify())
                .build();
    }

}
