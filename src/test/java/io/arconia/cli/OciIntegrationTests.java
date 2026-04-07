package io.arconia.cli;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class OciIntegrationTests {

    @Container
    public static final GenericContainer<?> zotContainer = new GenericContainer<>("ghcr.io/project-zot/zot-minimal:v2.1.15")
            .withExposedPorts(5000);

}
