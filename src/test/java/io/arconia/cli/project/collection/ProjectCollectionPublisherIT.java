package io.arconia.cli.project.collection;

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
 * Integration tests for {@link ProjectCollectionPublisher}.
 */
@DisabledInNativeImage
class ProjectCollectionPublisherIT extends OciIntegrationTests {

    @TempDir
    Path projectDir;

    private ProjectCollectionPublisher publisher;
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
        publisher = new ProjectCollectionPublisher(registry, createOutputOptions());
        projectPublisher = new ProjectPublisher(registry, createOutputOptions());

        Files.writeString(projectDir.resolve("README.md"), "# Test project");
        Files.writeString(projectDir.resolve(ProjectConfigParser.CONFIG_FILE_NAME), """
                name: my-project
                description: A test project
                type: service
                license: Apache-2.0
                """);
    }

    @Test
    void publishCollectionFromProjectRefsReturnsReport() throws IOException {
        String collectionName = "spring-templates";
        String projectRef = collectionRef(collectionName) + ":project-1.0.0";
        pushProject(collectionName);

        ProjectCollectionPushReport report = publisher.publish(
                projectsArguments(collectionName, "1.0.0", List.of(projectRef)));

        assertThat(report.artifacts()).hasSize(1);

        ProjectCollectionPushReport.ArtifactEntry entry = report.artifacts().getFirst();
        assertThat(entry.name()).isEqualTo(collectionName);
        assertThat(entry.description()).isEqualTo("A collection of templates");
        assertThat(entry.tag()).isEqualTo("1.0.0");
        assertThat(entry.digest()).startsWith("sha256:");
        assertThat(entry.ref()).contains(registryUrl).contains(collectionName + ":1.0.0");
    }

    @Test
    void publishCollectionFromReportFileReturnsReport() throws IOException {
        String collectionName = "data-templates";
        Path reportFile = saveReportToFile(pushProject(collectionName), "project-push-report.json");

        ProjectCollectionPushReport report = publisher.publish(
                fromReportArguments(collectionName, "1.0.0", reportFile));

        assertThat(report.artifacts()).hasSize(1);

        ProjectCollectionPushReport.ArtifactEntry entry = report.artifacts().getFirst();
        assertThat(entry.name()).isEqualTo(collectionName);
        assertThat(entry.description()).isEqualTo("A collection of templates");
        assertThat(entry.tag()).isEqualTo("1.0.0");
        assertThat(entry.digest()).startsWith("sha256:");
        assertThat(entry.ref()).contains(registryUrl).contains(collectionName + ":1.0.0");
    }

    @Test
    void publishCollectionIncludesAnnotationsInIndex() throws IOException {
        String collectionName = "annotated-templates";
        Path reportFile = saveReportToFile(pushProject(collectionName), "project-push-report.json");
        String ref = collectionRef(collectionName);

        publisher.publish(ProjectCollectionPushArguments.builder()
                .ref(ref)
                .tag("1.0.0")
                .name(collectionName)
                .description("A collection of templates")
                .annotations(Map.of("custom.team", "platform"))
                .fromReport(reportFile.toString())
                .build());

        Index fetchedIndex = registry.getIndex(ContainerRef.parse(ref + ":1.0.0"));
        assertThat(fetchedIndex.getAnnotations())
                .containsEntry("custom.team", "platform")
                .containsEntry(ArtifactAnnotations.OCI_TITLE, collectionName);
    }

    @Test
    void publishCollectionSavesReportFileWhenSet() throws IOException {
        String collectionName = "report-collection";
        Path reportFile = saveReportToFile(pushProject(collectionName), "project-push-report.json");
        Path collectionReportFile = projectDir.resolve("collection-push-report.json");

        publisher.publish(ProjectCollectionPushArguments.builder()
                .ref(collectionRef(collectionName))
                .tag("1.0.0")
                .name(collectionName)
                .description("A collection of templates")
                .annotations(Map.of())
                .fromReport(reportFile.toString())
                .reportFileName(collectionReportFile.toString())
                .build());

        assertThat(collectionReportFile).exists();
        ProjectCollectionPushReport loaded = ProjectCollectionPushReport.load(collectionReportFile);
        assertThat(loaded.artifacts()).hasSize(1);
        assertThat(loaded.artifacts().getFirst().name()).isEqualTo(collectionName);
    }

    @Test
    void publishCollectionDoesNotSaveReportFileWhenAbsent() throws IOException {
        String collectionName = "no-report-collection";
        Path reportFile = saveReportToFile(pushProject(collectionName), "project-push-report.json");

        publisher.publish(fromReportArguments(collectionName, "1.0.0", reportFile));

        assertThat(projectDir.resolve(ProjectCollectionPushReport.DEFAULT_FILENAME)).doesNotExist();
    }

    private ProjectPushReport pushProject(String collectionName) throws IOException {
        return projectPublisher.publish(ProjectPushArguments.builder()
                .ref(collectionRef(collectionName))
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

    private String collectionRef(String name) {
        return "%s/test/collections/%s".formatted(registryUrl, name);
    }

    private ProjectCollectionPushArguments projectsArguments(String name, String tag, List<String> projects) {
        return ProjectCollectionPushArguments.builder()
                .ref(collectionRef(name))
                .tag(tag)
                .name(name)
                .description("A collection of templates")
                .annotations(Map.of())
                .projects(projects)
                .build();
    }

    private ProjectCollectionPushArguments fromReportArguments(String name, String tag, Path reportFile) {
        return ProjectCollectionPushArguments.builder()
                .ref(collectionRef(name))
                .tag(tag)
                .name(name)
                .description("A collection of templates")
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
