package io.arconia.cli.aot;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.hint.ResourcePatternHints;
import org.springframework.aot.hint.RuntimeHints;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaCliRuntimeHints}.
 */
class ArconiaCliRuntimeHintsTests {

    @Test
    void shouldRegisterOpenRewriteInitScript() {
        RuntimeHints hints = register();
        assertThat(hints.resources().resourcePatternHints()).singleElement()
            .satisfies(include("openrewrite/init-rewrite.gradle"));
    }

    private RuntimeHints register() {
        RuntimeHints hints = new RuntimeHints();
        new ArconiaCliRuntimeHints().registerHints(hints, getClass().getClassLoader());
        return hints;
    }

    private Consumer<ResourcePatternHints> include(String... patterns) {
        return (hint) -> assertThat(hint.getIncludes()).map(ResourcePatternHint::getPattern).contains(patterns);
    }

}
