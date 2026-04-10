package io.arconia.cli.project.catalog;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.project.ProjectPublisher;
import io.arconia.cli.project.ProjectPushArguments;
import io.arconia.cli.project.ProjectPushReport;
import io.arconia.cli.project.oci.ProjectConfigParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ProjectCatalogPublisher}.
 */
@DisabledInNativeImage
class ProjectCatalogPublisherIT extends OciIntegrationTests {

    @TempDir
    Path projectDir;

    private ProjectCatalogPublisher publisher;
    private ProjectPublisher projectPublisher;
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
        publisher = new ProjectCatalogPublisher(registry, createOutputOptions());
        projectPublisher = new ProjectPublisher(registry, createOutputOptions());

        Files.writeString(projectDir.resolve("README.md"), "# Test project");
        Files.writeString(projectDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), """
                schemaVersion: "1"
                name: my-project
                description: A test project
                type: service
                license: Apache-2.0
                """);
    }

    @Test
    void publishCatalogFromProjectRefsReturnsReport() throws IOException {
        String catalogName = "spring-templates";
        String projectRef = catalogRef(catalogName) + ":project-1.0.0";
        pushProject(catalogName);

        ProjectCatalogPushReport report = publisher.publish(
                projectsArguments(catalogName, "1.0.0", List.of(projectRef)));

        assertThat(report.artifacts()).hasSize(1);

        ProjectCatalogPushReport.ArtifactEntry entry = report.artifacts().getFirst();
        assertThat(entry.name()).isEqualTo(catalogName);
        assertThat(entry.description()).isEqualTo("A catalog of templates");
        assertThat(entry.tag()).isEqualTo("1.0.0");
        assertThat(entry.digest()).startsWith("sha256:");
        assertThat(entry.ref()).contains(registryUrl).contains(catalogName + ":1.0.0");
    }

    @Test
    void publishCatalogFromReportFileReturnsReport() throws IOException {
        String catalogName = "data-templates";
        Path reportFile = saveReportToFile(pushProject(catalogName), "template-push-report.json");

        ProjectCatalogPushReport report = publisher.publish(
                fromReportArguments(catalogName, "1.0.0", reportFile));

        assertThat(report.artifacts()).hasSize(1);

        ProjectCatalogPushReport.ArtifactEntry entry = report.artifacts().getFirst();
        assertThat(entry.name()).isEqualTo(catalogName);
        assertThat(entry.description()).isEqualTo("A catalog of templates");
        assertThat(entry.tag()).isEqualTo("1.0.0");
        assertThat(entry.digest()).startsWith("sha256:");
        assertThat(entry.ref()).contains(registryUrl).contains(catalogName + ":1.0.0");
    }

    @Test
    void publishCatalogIncludesAnnotationsInIndex() throws IOException {
        String catalogName = "annotated-templates";
        Path reportFile = saveReportToFile(pushProject(catalogName), "template-push-report.json");
        String ref = catalogRef(catalogName);

        publisher.publish(ProjectCatalogPushArguments.builder()
                .ref(ref)
                .tag("1.0.0")
                .name(catalogName)
                .description("A catalog of templates")
                .annotations(Map.of("custom.team", "platform"))
                .fromReport(reportFile.toString())
                .build());

        Index fetchedIndex = registry.getIndex(ContainerRef.parse(ref + ":1.0.0"));
        assertThat(fetchedIndex.getAnnotations())
                .containsEntry("custom.team", "platform")
                .containsEntry(ArtifactAnnotations.OCI_TITLE, catalogName);
    }

    @Test
    void publishCatalogSavesReportFileWhenSet() throws IOException {
        String catalogName = "report-catalog";
        Path reportFile = saveReportToFile(pushProject(catalogName), "template-push-report.json");
        Path catalogReportFile = projectDir.resolve("catalog-push-report.json");

        publisher.publish(ProjectCatalogPushArguments.builder()
                .ref(catalogRef(catalogName))
                .tag("1.0.0")
                .name(catalogName)
                .description("A catalog of templates")
                .annotations(Map.of())
                .fromReport(reportFile.toString())
                .reportFileName(catalogReportFile.toString())
                .build());

        assertThat(catalogReportFile).exists();
        ProjectCatalogPushReport loaded = ProjectCatalogPushReport.load(catalogReportFile);
        assertThat(loaded.artifacts()).hasSize(1);
        assertThat(loaded.artifacts().getFirst().name()).isEqualTo(catalogName);
    }

    @Test
    void publishCatalogDoesNotSaveReportFileWhenAbsent() throws IOException {
        String catalogName = "no-report-catalog";
        Path reportFile = saveReportToFile(pushProject(catalogName), "template-push-report.json");

        publisher.publish(fromReportArguments(catalogName, "1.0.0", reportFile));

        assertThat(projectDir.resolve(ProjectCatalogPushReport.DEFAULT_FILENAME)).doesNotExist();
    }

    private ProjectPushReport pushProject(String catalogName) throws IOException {
        return projectPublisher.publish(ProjectPushArguments.builder()
                .ref(catalogRef(catalogName))
                .tag("project-1.0.0")
                .annotations(Map.of())
                .projectPath(projectDir)
                .build());
    }

    private Path saveReportToFile(ProjectPushReport report, String fileName) throws IOException {
        Path reportFile = projectDir.resolve(fileName);
        report.save(reportFile);
        return reportFile;
    }

    private String catalogRef(String name) {
        return "%s/test/catalogs/%s".formatted(registryUrl, name);
    }

    private ProjectCatalogPushArguments projectsArguments(String name, String tag, List<String> projects) {
        return ProjectCatalogPushArguments.builder()
                .ref(catalogRef(name))
                .tag(tag)
                .name(name)
                .description("A catalog of templates")
                .annotations(Map.of())
                .projects(projects)
                .build();
    }

    private ProjectCatalogPushArguments fromReportArguments(String name, String tag, Path reportFile) {
        return ProjectCatalogPushArguments.builder()
                .ref(catalogRef(name))
                .tag(tag)
                .name(name)
                .description("A catalog of templates")
                .annotations(Map.of())
                .fromReport(reportFile.toString())
                .build();
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
