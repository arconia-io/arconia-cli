package io.arconia.cli.artifact;

import land.oras.Registry;

/**
 * Central factory for creating ORAS registry clients.
 */
public final class ArtifactRegistry {

    private ArtifactRegistry() {}

    /**
     * Creates an ORAS registry client with default settings.
     *
     * @return a configured registry client
     */
    public static Registry create() {
        return Registry.builder().defaults().build();
    }

}
