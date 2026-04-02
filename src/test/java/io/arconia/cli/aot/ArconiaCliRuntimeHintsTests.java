package io.arconia.cli.aot;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.hint.ResourcePatternHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaCliRuntimeHints}.
 */
@DisabledInNativeImage
class ArconiaCliRuntimeHintsTests {

    @Test
    void shouldRegisterOpenRewriteInitScript() {
        RuntimeHints hints = register();
        assertThat(hints.resources().resourcePatternHints()).singleElement()
            .satisfies(include("openrewrite/init-rewrite.gradle"));
    }

    @Test
    void shouldDiscoverSkillsClasses() {
        var orasModelClasses = ArconiaCliRuntimeHints.findSkillsClasses();
        assertThat(orasModelClasses).isNotEmpty();
        assertThat(orasModelClasses).contains(
                TypeReference.of("io.arconia.cli.skills.SkillCollectionRegistry"),
                TypeReference.of("io.arconia.cli.skills.SkillCollectionRegistry$CollectionEntry")
        );
    }

    @Test
    void shouldRegisterReflectionHintsForSkillsClasses() {
        RuntimeHints hints = register();
        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TypeReference.of("io.arconia.cli.skills.SkillCollectionRegistry")).test(hints)).isTrue();
        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TypeReference.of("io.arconia.cli.skills.SkillCollectionRegistry$CollectionEntry")).test(hints)).isTrue();
    }

    @Test
    void shouldDiscoverOrasModelClasses() {
        var orasModelClasses = ArconiaCliRuntimeHints.findOrasModelClasses();
        assertThat(orasModelClasses).isNotEmpty();
        assertThat(orasModelClasses).contains(
            TypeReference.of("land.oras.Config"),
            TypeReference.of("land.oras.auth.AuthStore$ConfigFile"),
            TypeReference.of("land.oras.exception.Error")
        );
    }

    @Test
    void shouldRegisterReflectionHintsForOrasModelClasses() {
        RuntimeHints hints = register();
        assertThat(RuntimeHintsPredicates.reflection()
            .onType(TypeReference.of("land.oras.Config")).test(hints)).isTrue();
        assertThat(RuntimeHintsPredicates.reflection()
            .onType(TypeReference.of("land.oras.auth.AuthStore$ConfigFile")).test(hints)).isTrue();
        assertThat(RuntimeHintsPredicates.reflection()
            .onType(TypeReference.of("land.oras.exception.Error")).test(hints)).isTrue();
    }

    @Test
    void shouldRegisterReflectionHintsForCaffeineClasses() {
        RuntimeHints hints = register();
        assertThat(RuntimeHintsPredicates.reflection()
            .onType(TypeReference.of("com.github.benmanes.caffeine.cache.SSMS")).test(hints)).isTrue();
        assertThat(RuntimeHintsPredicates.reflection()
            .onType(TypeReference.of("com.github.benmanes.caffeine.cache.SSSMSA")).test(hints)).isTrue();
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
