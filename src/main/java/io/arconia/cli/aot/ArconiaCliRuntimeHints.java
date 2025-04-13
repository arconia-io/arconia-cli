package io.arconia.cli.aot;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;

public class ArconiaCliRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.resources().registerResource(new ClassPathResource("openrewrite/init-arconia.gradle"));
        hints.resources().registerResource(new ClassPathResource("openrewrite/init-generic.gradle"));
        hints.resources().registerResource(new ClassPathResource("openrewrite/init-openrewrite.gradle"));
    }

}
