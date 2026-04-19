package io.arconia.cli.project;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import land.oras.ContainerRef;
import land.oras.Manifest;
import land.oras.Registry;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import io.arconia.cli.OciIntegrationTests;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.project.oci.ProjectConfig;
import io.arconia.cli.project.oci.ProjectConfigParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ProjectPublisher}.
 */
class ProjectPublisherIT extends OciIntegrationTests {

    @TempDir
    Path projectDir;

    private ProjectPublisher publisher;
    private Registry registry;
    private String registryUrl;

    @BeforeEach
    void setUp() throws IOException {
        registryUrl = zotContainer.getHost() + ":" + zotContainer.getMappedPort(5000);
        registry = Registry.builder()
                .withRegistry(registryUrl)
                .withInsecure(true)
                .withSkipTlsVerify(true)
                .build();
        publisher = new ProjectPublisher(registry, createOutputOptions());

        Files.writeString(projectDir.resolve("README.md"), "# Test project");
        Files.writeString(projectDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), """
                schemaVersion: "1"
                name: my-app
                description: A test application
                type: service
                license: Apache-2.0
                """);
    }

    @Test
    void publishReturnsReportWithMetadata() throws IOException {
        ProjectPushReport report = publisher.publish(pushArguments("my-app", "1.0.0"));

        assertThat(report.artifacts()).hasSize(1);

        ProjectPushReport.ArtifactEntry entry = report.artifacts().getFirst();
        assertThat(entry.name()).isEqualTo("my-app");
        assertThat(entry.description()).isEqualTo("A test application");
        assertThat(entry.type()).isEqualTo("service");
        assertThat(entry.license()).isEqualTo("Apache-2.0");
        assertThat(entry.tag()).isEqualTo("1.0.0");
        assertThat(entry.digest()).startsWith("sha256:");
        assertThat(entry.ref())
                .contains(registryUrl)
                .contains("my-app:1.0.0");
    }

    @Test
    void publishWithExplicitProjectConfigSkipsConfigFileParsing() throws IOException {
        ProjectConfig config = ProjectConfig.builder()
                .name("explicit-app")
                .description("Provided explicitly")
                .type("library")
                .license("MIT")
                .build();

        ProjectPushArguments args = pushArguments("explicit-app", "2.0.0");
        ProjectPushReport report = publisher.publish(args, config);

        ProjectPushReport.ArtifactEntry entry = report.artifacts().getFirst();
        assertThat(entry.name()).isEqualTo("explicit-app");
        assertThat(entry.description()).isEqualTo("Provided explicitly");
        assertThat(entry.type()).isEqualTo("library");
        assertThat(entry.license()).isEqualTo("MIT");
    }

    @Test
    void publishWithAdditionalAnnotationsIncludesThemInManifest() throws IOException {
        String ref = "%s/test/my-app-annotated".formatted(registryUrl);
        ProjectPushArguments args = ProjectPushArguments.builder()
                .ref(ref)
                .tag("1.0.0")
                .annotations(Map.of("custom.team", "platform"))
                .projectPath(projectDir)
                .build();

        ProjectConfig config = ProjectConfig.builder()
                .name("my-app-annotated")
                .description("A test application")
                .build();

        publisher.publish(args, config);

        Manifest manifest = registry.getManifest(ContainerRef.parse(ref + ":1.0.0"));
        assertThat(manifest.getAnnotations()).containsEntry("custom.team", "platform");
    }

    @Test
    void publishSavesReportFileWhenReportFileNameIsSet() throws IOException {
        Path reportFile = projectDir.resolve("push-report.json");
        ProjectPushArguments args = ProjectPushArguments.builder()
                .ref("%s/test/my-app-report".formatted(registryUrl))
                .tag("1.0.0")
                .annotations(Map.of())
                .projectPath(projectDir)
                .reportFileName(reportFile.toString())
                .build();

        ProjectConfig config = ProjectConfig.builder()
                .name("my-app-report")
                .description("A test application")
                .build();

        publisher.publish(args, config);

        assertThat(reportFile).exists();
        ProjectPushReport loaded = ProjectPushReport.load(reportFile);
        assertThat(loaded.artifacts()).hasSize(1);
        assertThat(loaded.artifacts().getFirst().name()).isEqualTo("my-app-report");
    }

    @Test
    void publishDoesNotSaveReportFileWhenReportFileNameIsAbsent() throws IOException {
        publisher.publish(pushArguments("my-app-no-report", "1.0.0"));

        assertThat(projectDir.resolve(ProjectPushReport.DEFAULT_FILENAME)).doesNotExist();
    }

    private ProjectPushArguments pushArguments(String name, String tag) {
        return ProjectPushArguments.builder()
                .ref("%s/test/%s".formatted(registryUrl, name))
                .tag(tag)
                .annotations(Map.of())
                .projectPath(projectDir)
                .build();
    }

    private OutputOptions createOutputOptions() {
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
