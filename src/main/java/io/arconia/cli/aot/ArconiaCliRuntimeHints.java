package io.arconia.cli.aot;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import land.oras.OrasModel;

/**
 * {@link RuntimeHintsRegistrar} for locating the classpath resources required by the Arconia CLI.
 */
public class ArconiaCliRuntimeHints implements RuntimeHintsRegistrar {

    /**
     * Caffeine cache classes that need additional reflection hints beyond what the
     * GraalVM Reachability Metadata Repository provides for Caffeine 3.1.2.
     * <p>
     * The ORAS Java SDK's token cache ({@code land.oras.auth.TokenCache}) builds
     * a bounded manual cache via {@code Caffeine.newBuilder().maximumSize(...).recordStats()
     * .expireAfter(Expiry).build()}. This goes through {@code BoundedLocalManualCache},
     * but the GraalVM metadata repo only registers the {@code SSMS} constructor under
     * the {@code BoundedLocalLoadingCache} condition (for {@code .build(loader)}),
     * so it is never triggered for manual caches.
     * <p>
     * Additionally, {@code SSSMSA} is a cache class introduced in Caffeine 3.2.x
     * that is not present in the 3.1.2 metadata at all.
     */
    private static final List<String> CAFFEINE_CLASSES = List.of(
        "com.github.benmanes.caffeine.cache.SSMS",
        "com.github.benmanes.caffeine.cache.SSSMSA"
    );

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.resources()
                .registerResource(new ClassPathResource("openrewrite/init-rewrite.gradle"));
        findSkillsClasses().forEach(type -> hints.reflection()
                .registerType(type, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS));
        findOrasModelClasses().forEach(type -> hints.reflection()
                .registerType(type, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.ACCESS_DECLARED_FIELDS));
        CAFFEINE_CLASSES.forEach(className -> hints.reflection()
                .registerType(TypeReference.of(className),
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.ACCESS_DECLARED_FIELDS));
    }

    /**
     * Scans the {@code io.arconia.cli.skills} package for classes annotated with {@link JsonIgnoreProperties @JsonIgnoreProperties}.
     */
    static Set<TypeReference> findSkillsClasses() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(JsonIgnoreProperties.class));
        return scanner.findCandidateComponents("io.arconia.cli.skills")
                .stream()
                .map(bd -> TypeReference.of(Objects.requireNonNull(bd.getBeanClassName())))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Scans the {@code land.oras} package for classes annotated with {@link OrasModel @OrasModel}.
     */
    static Set<TypeReference> findOrasModelClasses() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(OrasModel.class));
        return scanner.findCandidateComponents("land.oras")
            .stream()
            .map(bd -> TypeReference.of(Objects.requireNonNull(bd.getBeanClassName())))
            .collect(Collectors.toUnmodifiableSet());
    }

}
