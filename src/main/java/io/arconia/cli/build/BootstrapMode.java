package io.arconia.cli.build;

public enum BootstrapMode {

    DEV,
    TEST,
    PROD;

    /**
     * The environment variable key used to configure the bootstrap mode
     * for a Spring Boot application using Arconia.
     */
    public static final String ENV_VAR_KEY = "ARCONIA_BOOTSTRAP_MODE";

}
