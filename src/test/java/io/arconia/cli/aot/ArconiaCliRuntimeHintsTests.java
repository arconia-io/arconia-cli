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
    void shouldRegisterResources() {
        RuntimeHints hints = register();
        assertThat(hints.resources().resourcePatternHints()).hasSize(2)
                .anySatisfy(include("openrewrite/init-rewrite.gradle"))
                .anySatisfy(include("recipes/project-customization.yml"));
    }

    @Test
    void shouldRegisterReflectionHintsForArconiaClasses() {
        RuntimeHints hints = register();
        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TypeReference.of("io.arconia.cli.project.oci.ProjectConfig")).test(hints)).isTrue();
        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TypeReference.of("io.arconia.cli.project.ProjectPushReport$ArtifactEntry")).test(hints)).isTrue();
        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TypeReference.of("io.arconia.cli.skills.SkillCollectionRegistry")).test(hints)).isTrue();
        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TypeReference.of("io.arconia.cli.skills.SkillCollectionRegistry$CollectionEntry")).test(hints)).isTrue();
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
