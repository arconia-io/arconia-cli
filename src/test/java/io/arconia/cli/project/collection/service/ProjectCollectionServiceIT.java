package io.arconia.cli.project.collection.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.io.TempDir;

import land.oras.ContainerRef;
import land.oras.Index;
import land.oras.Registry;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import io.arconia.cli.OciIntegrationTests;
import io.arconia.cli.commands.options.OutputOptions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ProjectCollectionService}.
 */
@DisabledInNativeImage
class ProjectCollectionServiceIT extends OciIntegrationTests {

    @TempDir
    Path tempDir;

    private ProjectCollectionService service;
    private Registry registry;
    private String registryUrl;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("arconia.config.home", tempDir.resolve("config").toString());
        System.setProperty("arconia.cache.home", tempDir.resolve("cache").toString());

        registryUrl = zotContainer.getHost() + ":" + zotContainer.getMappedPort(5000);
        registry = Registry.builder()
                .withRegistry(registryUrl)
                .withInsecure(true)
                .withSkipTlsVerify(true)
                .build();
        service = new ProjectCollectionService(registry, createOutputOptions());
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("arconia.config.home");
        System.clearProperty("arconia.cache.home");
    }

    @Test
    void addCollectionSavesToRegistryAndCache() throws IOException {
        String collectionRef = pushEmptyCollection("spring-templates", "1.0.0");

        service.addCollection("spring-templates", collectionRef);

        Index cached = ProjectCollectionCache.load("spring-templates");
        assertThat(cached).isNotNull();

        ProjectCollectionRegistry.CollectionEntry entry = ProjectCollectionRegistry.findCollection("spring-templates");
        assertThat(entry).isNotNull();
        assertThat(entry.name()).isEqualTo("spring-templates");
        assertThat(entry.ref()).isEqualTo(collectionRef);
        assertThat(entry.digest()).startsWith("sha256:");
    }

    @Test
    void removeCollectionDeletesFromRegistryAndCache() throws IOException {
        String collectionRef = pushEmptyCollection("data-templates", "1.0.0");
        service.addCollection("data-templates", collectionRef);

        service.removeCollection("data-templates");

        Index cached = ProjectCollectionCache.load("data-templates");
        assertThat(cached).isNull();

        ProjectCollectionRegistry.CollectionEntry entry = ProjectCollectionRegistry.findCollection("data-templates");
        assertThat(entry).isNull();
    }

    @Test
    void removeCollectionIsIdempotentWhenNotPresent() throws IOException {
        service.removeCollection("nonexistent-collection");

        ProjectCollectionRegistry.CollectionEntry entry = ProjectCollectionRegistry.findCollection("nonexistent-collection");
        assertThat(entry).isNull();
    }

    private String pushEmptyCollection(String name, String tag) {
        String ref = "%s/test/collections/%s".formatted(registryUrl, name);
        ContainerRef containerRef = ContainerRef.parse(ref).withTag(tag);
        registry.pushIndex(containerRef, Index.fromManifests(List.of()));
        return ref + ":" + tag;
    }

    private static OutputOptions createOutputOptions() {
        TestCommand testCommand = new TestCommand();
        new CommandLine(testCommand)
                .setOut(new PrintWriter(new StringWriter()))
                .setErr(new PrintWriter(new StringWriter()))
                .parseArgs();
        return testCommand.outputOptions;
    }

    @Command(name = "test")
    private static class TestCommand implements Runnable {
        @Mixin
        OutputOptions outputOptions = new OutputOptions();

        @Override
        public void run() {}
    }

}
