package io.arconia.cli.oci;

import land.oras.Registry;

/**
 * Central factory for creating ORAS registry clients.
 */
public final class OciRegistryProvider {

    private OciRegistryProvider() {}

    /**
     * Creates an ORAS registry client with default settings.
     *
     * @return a configured registry client
     */
    public static Registry create() {
        return Registry.builder().defaults().build();
    }

}
