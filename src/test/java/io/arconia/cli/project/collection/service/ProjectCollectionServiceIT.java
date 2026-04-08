package io.arconia.cli.project.collection.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.io.TempDir;

import land.oras.Annotations;
import land.oras.ArtifactType;
import land.oras.ContainerRef;
import land.oras.Index;
import land.oras.Manifest;
import land.oras.ManifestDescriptor;
import land.oras.Registry;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import io.arconia.cli.OciIntegrationTests;
import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.artifact.ArtifactInfo;
import io.arconia.cli.artifact.ArtifactMediaTypes;
import io.arconia.cli.artifact.ArtifactPublisher;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.project.oci.ProjectAnnotations;
import io.arconia.cli.project.oci.ProjectConfig;
import io.arconia.cli.project.oci.ProjectMediaTypes;

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
    private StringWriter outWriter;
    private StringWriter errWriter;

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

        outWriter = new StringWriter();
        errWriter = new StringWriter();
        service = new ProjectCollectionService(registry, createOutputOptions(outWriter, errWriter));
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

    @Test
    void updateCollectionRefreshesRegistryAndCache() throws IOException {
        String refV1 = pushEmptyCollection("update-templates", "1.0.0");
        service.addCollection("update-templates", refV1);
        ProjectCollectionRegistry.CollectionEntry collectionV1 = ProjectCollectionRegistry.findCollection("update-templates");
        assertThat(collectionV1).isNotNull();
        String digestV1 = collectionV1.digest();

        String refV2 = pushCollectionWithAnnotations("update-templates", "1.1.0", Map.of("key", "value"));

        service.updateCollection("update-templates");

        Index cached = ProjectCollectionCache.load("update-templates");
        assertThat(cached).isNotNull();

        ProjectCollectionRegistry.CollectionEntry entry = ProjectCollectionRegistry.findCollection("update-templates");
        assertThat(entry).isNotNull();
        assertThat(entry.ref()).isEqualTo(refV2);
        assertThat(entry.digest()).startsWith("sha256:");
        assertThat(entry.digest()).isNotEqualTo(digestV1);
    }

    @Test
    void updateCollectionDoesNothingWhenNotRegistered() throws IOException {
        service.updateCollection("nonexistent-collection");

        ProjectCollectionRegistry.CollectionEntry entry = ProjectCollectionRegistry.findCollection("nonexistent-collection");
        assertThat(entry).isNull();
    }

    @Test
    void listCollectionDoesNothingWhenNotRegistered() throws IOException {
        service.listCollection("nonexistent-collection");

        String errOutput = errWriter.toString();
        assertThat(errOutput).contains("nonexistent-collection");
        assertThat(errOutput).contains("not registered");

        ProjectCollectionRegistry.CollectionEntry entry = ProjectCollectionRegistry.findCollection("nonexistent-collection");
        assertThat(entry).isNull();
    }

    @Test
    void listCollectionDisplaysEmptyCollection() throws IOException {
        String collectionRef = pushCollectionWithAnnotations("list-empty", "1.0.0",
                Map.of(ArtifactAnnotations.OCI_DESCRIPTION, "An empty collection"));
        service.addCollection("list-empty", collectionRef);
        outWriter.getBuffer().setLength(0);

        service.listCollection("list-empty");

        String output = outWriter.toString();
        assertThat(output).contains("list-empty");
        assertThat(output).contains(collectionRef);
        assertThat(output).contains("An empty collection");
        assertThat(output).contains("NAME");
        assertThat(output).contains("DESCRIPTION");
        assertThat(output).contains("TYPE");
        assertThat(output).contains("LABELS");
    }

    @Test
    void listCollectionDisplaysCollectionWithProjects() throws IOException {
        String collectionRef = pushCollectionWithProjects("list-projects", "1.0.0");
        service.addCollection("list-projects", collectionRef);
        outWriter.getBuffer().setLength(0);

        service.listCollection("list-projects");

        String output = outWriter.toString();
        assertThat(output).contains("list-projects");
        assertThat(output).contains("A collection with projects");
        assertThat(output).contains("my-api");
        assertThat(output).contains("A REST API service");
        assertThat(output).contains("service");
        assertThat(output).contains("my-web");
        assertThat(output).contains("A web frontend");
        assertThat(output).contains("application");
    }

    private String pushEmptyCollection(String name, String tag) {
        String ref = "%s/test/collections/%s".formatted(registryUrl, name);
        ContainerRef containerRef = ContainerRef.parse(ref).withTag(tag);
        registry.pushIndex(containerRef, Index.fromManifests(List.of()));
        return ref + ":" + tag;
    }

    private String pushCollectionWithAnnotations(String name, String tag, Map<String, String> annotations) {
        String ref = "%s/test/collections/%s".formatted(registryUrl, name);
        ContainerRef containerRef = ContainerRef.parse(ref).withTag(tag);
        registry.pushIndex(containerRef, Index.fromManifests(List.of()).withAnnotations(annotations));
        return "%s/%s:%s".formatted(containerRef.getRegistry(), containerRef.getFullRepository(), tag);
    }

    private String pushCollectionWithProjects(String collectionName, String tag) throws IOException {
        Path contentDir = tempDir.resolve("project-content");
        Files.createDirectories(contentDir);
        Files.writeString(contentDir.resolve("README.md"), "# Test project");

        // Push project manifests into the same repository as the collection
        // (OCI registries require referenced manifests to exist in the same repo)
        String collectionRepo = "test/collections/%s".formatted(collectionName);
        ArtifactPublisher publisher = new ArtifactPublisher(registry);

        Manifest manifest1 = publisher.publish(buildProjectArtifactInfo(contentDir, collectionRepo,
                "my-api", "A REST API service", "service", List.of("java", "spring"), "my-api-1.0.0"));
        Manifest manifest2 = publisher.publish(buildProjectArtifactInfo(contentDir, collectionRepo,
                "my-web", "A web frontend", "application", List.of("typescript", "react"), "my-web-2.0.0"));

        // Build ManifestDescriptors with real digests
        ManifestDescriptor project1 = buildProjectDescriptor("my-api", "A REST API service", "service",
                "java,spring", "1.0.0", "ghcr.io/org/projects/my-api:1.0.0",
                manifest1.getDescriptor().getDigest(), manifest1.getDescriptor().getSize());
        ManifestDescriptor project2 = buildProjectDescriptor("my-web", "A web frontend", "application",
                "typescript,react", "2.0.0", "ghcr.io/org/projects/my-web:2.0.0",
                manifest2.getDescriptor().getDigest(), manifest2.getDescriptor().getSize());

        // Push the collection index referencing the real manifests
        String ref = "%s/%s".formatted(registryUrl, collectionRepo);
        ContainerRef containerRef = ContainerRef.parse(ref).withTag(tag);
        Index index = Index.fromManifests(List.of(project1, project2))
                .withAnnotations(Map.of(ArtifactAnnotations.OCI_DESCRIPTION, "A collection with projects"));
        registry.pushIndex(containerRef, index);
        return "%s/%s:%s".formatted(containerRef.getRegistry(), containerRef.getFullRepository(), tag);
    }

    private ArtifactInfo buildProjectArtifactInfo(Path contentDir, String repo, String name, String description,
            String type, List<String> labels, String tag) {
        ProjectConfig config = ProjectConfig.builder()
                .name(name)
                .description(description)
                .type(type)
                .labels(labels)
                .build();

        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put(ArtifactAnnotations.OCI_TITLE, name);
        annotations.put(ArtifactAnnotations.OCI_DESCRIPTION, description);
        annotations.put(ArtifactAnnotations.OCI_VERSION, tag);
        annotations.put(ProjectAnnotations.PROJECT_TYPE, type);
        annotations.put(ProjectAnnotations.PROJECT_LABELS, String.join(",", labels));

        return ArtifactInfo.builder()
                .config(config.toConfig())
                .annotations(Annotations.ofManifest(annotations))
                .path(contentDir)
                .containerRef(ContainerRef.parse("%s/%s:%s".formatted(registryUrl, repo, tag)))
                .artifactType(ArtifactType.from(ProjectMediaTypes.PROJECT_ARTIFACT_TYPE))
                .contentType(ProjectMediaTypes.PROJECT_ARTIFACT_CONTENT_LAYER)
                .build();
    }

    private static ManifestDescriptor buildProjectDescriptor(String name, String description, String type,
            String labels, String version, String refName, String digest, long size) {
        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put(ArtifactAnnotations.OCI_TITLE, name);
        annotations.put(ArtifactAnnotations.OCI_DESCRIPTION, description);
        annotations.put(ArtifactAnnotations.OCI_VERSION, version);
        annotations.put(ArtifactAnnotations.OCI_REF_NAME, refName);
        annotations.put(ProjectAnnotations.PROJECT_TYPE, type);
        annotations.put(ProjectAnnotations.PROJECT_LABELS, labels);

        return ManifestDescriptor.of(ArtifactMediaTypes.IMAGE_MANIFEST, digest, size)
                .withAnnotations(annotations);
    }

    private static OutputOptions createOutputOptions(StringWriter outWriter, StringWriter errWriter) {
        TestCommand testCommand = new TestCommand();
        new CommandLine(testCommand)
                .setOut(new PrintWriter(outWriter))
                .setErr(new PrintWriter(errWriter))
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
