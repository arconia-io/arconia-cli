package io.arconia.cli.build;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.openrewrite.RewriteArguments;
import io.arconia.cli.openrewrite.UpdateArguments;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MavenRunner}.
 */
class MavenRunnerTests {

    @TempDir
    Path tempDir;

    MavenRunner runner;

    @BeforeEach
    void setUp() throws IOException {
        File wrapper = tempDir.resolve("mvnw").toFile();
        Files.createFile(wrapper.toPath());

        runner = new MavenRunner(createOutputOptions(), List.of(), tempDir);
    }

    private OutputOptions createOutputOptions() {
        var outputOptions = new OutputOptions();
        var spec = CommandSpec.create()
            .name("test-cmd")
            .addMixin("outputOptions", CommandSpec.forAnnotatedObject(outputOptions));
        var cmd = new CommandLine(spec);
        cmd.setOut(new PrintWriter(new StringWriter()));
        cmd.setErr(new PrintWriter(new StringWriter()));
        cmd.parseArgs();
        return outputOptions;
    }

    @Test
    void buildToolIsMaven() {
        assertThat(runner.getBuildTool()).isEqualTo(BuildTool.MAVEN);
    }

    // -- constructMavenCommand tests --

    @Test
    void buildCommandDefault() {
        var arguments = BuildArguments.builder().build();
        var command = runner.constructMavenCommand("package", arguments, BootstrapMode.PROD);

        assertThat(command).contains("package");
        assertThat(command).doesNotContain("clean", "-Pnative", "-DskipTests");
    }

    @Test
    void buildCommandWithClean() {
        var arguments = BuildArguments.builder().clean(true).build();
        var command = runner.constructMavenCommand("package", arguments, BootstrapMode.PROD);

        assertThat(command).containsSubsequence("clean", "package");
    }

    @Test
    void buildCommandWithSkipTests() {
        var arguments = BuildArguments.builder().skipTests(true).build();
        var command = runner.constructMavenCommand("package", arguments, BootstrapMode.PROD);

        assertThat(command).contains("-DskipTests", "-Dmaven.test.skip=true");
    }

    @Test
    void buildCommandWithNativeTrait() {
        var arguments = BuildArguments.builder().nativeBuild(true).build();
        var command = runner.constructMavenCommand("package", arguments, BootstrapMode.PROD);

        assertThat(command).contains("-Pnative");
    }

    @Test
    void buildCommandWithParams() {
        var runner = new MavenRunner(createOutputOptions(), List.of("-X", "-e"), tempDir);
        var command = runner.constructMavenCommand("package", BuildArguments.builder().build(), BootstrapMode.PROD);

        assertThat(command).contains("-X", "-e");
    }

    @Test
    void imageBuildCommandSkipsTestsAutomatically() {
        var arguments = BuildArguments.builder().build();
        var command = runner.constructMavenCommand("spring-boot:build-image", arguments, BootstrapMode.PROD);

        assertThat(command).contains("-DskipTests", "-Dmaven.test.skip=true");
    }

    @Test
    void buildCommandWithBuildImageOptions() {
        var imageArguments = BuildImageArguments.builder()
            .imageName("my-image")
            .builderImage("paketobuildpacks/builder")
            .runImage("paketobuildpacks/run")
            .cleanCache(true)
            .publishImage(true)
            .build();
        var arguments = BuildArguments.builder().buildImageArguments(imageArguments).build();
        var command = runner.constructMavenCommand("spring-boot:build-image", arguments, BootstrapMode.PROD);

        assertThat(command).contains(
            "-Dspring-boot.build-image.imageName=my-image",
            "-Dspring-boot.build-image.builder=paketobuildpacks/builder",
            "-Dspring-boot.build-image.runImage=paketobuildpacks/run",
            "-Dspring-boot.build-image.cleanCache=true",
            "-Dspring-boot.build-image.publish=true");
    }

    // -- constructRewriteCommand tests --

    @Test
    void rewriteRunCommandRun() {
        var options = RewriteArguments.builder()
            .rewriteRecipeName("org.example.MyRecipe")
            .build();
        var command = runner.constructRewriteRunCommand(options);

        assertThat(command).contains("-U");
        assertThat(command).contains("org.openrewrite.maven:rewrite-maven-plugin:%s:run".formatted(MavenRunner.OPEN_REWRITE_DEFAULT_VERSION));
        assertThat(command).contains("-Drewrite.activeRecipes=org.example.MyRecipe");
        assertThat(command).contains("-Drewrite.exportDatatables=true");
    }

    @Test
    void rewriteRunCommandDryRun() {
        var options = RewriteArguments.builder()
            .dryRun(true)
            .rewriteRecipeName("org.example.MyRecipe")
            .build();
        var command = runner.constructRewriteRunCommand(options);

        assertThat(command).contains("org.openrewrite.maven:rewrite-maven-plugin:%s:dry-run".formatted(MavenRunner.OPEN_REWRITE_DEFAULT_VERSION));
    }

    @Test
    void rewriteRunCommandWithRecipeLibrary() {
        var options = RewriteArguments.builder()
            .rewriteRecipeName("org.example.MyRecipe")
            .rewriteRecipeLibrary("org.example:my-recipes")
            .rewriteRecipeVersion("2.0.0")
            .build();
        var command = runner.constructRewriteRunCommand(options);

        assertThat(command).anySatisfy(arg -> assertThat(arg).contains("org.example:my-recipes:2.0.0"));    }

    // -- constructUpdateCommand tests --

    @Test
    void updateCommandRun() {
        var options = UpdateArguments.builder()
            .rewriteRecipeName("org.example.UpdateRecipe")
            .rewriteRecipeLibrary("org.example:update-recipes")
            .build();
        var command = runner.constructUpdateCommand(options);

        assertThat(command).contains("-U");
        assertThat(command).contains("org.openrewrite.maven:rewrite-maven-plugin:%s:run".formatted(MavenRunner.OPEN_REWRITE_DEFAULT_VERSION));
        assertThat(command).contains("-Drewrite.activeRecipes=org.example.UpdateRecipe");
        assertThat(command).contains("-Drewrite.recipeArtifactCoordinates=org.example:update-recipes:%s".formatted(MavenRunner.OPEN_REWRITE_DEFAULT_VERSION));
        assertThat(command).contains("-Drewrite.exportDatatables=true");
    }

    @Test
    void updateCommandDryRun() {
        var options = UpdateArguments.builder()
            .dryRun(true)
            .rewriteRecipeName("org.example.UpdateRecipe")
            .rewriteRecipeLibrary("org.example:update-recipes")
            .build();
        var command = runner.constructUpdateCommand(options);

        assertThat(command).contains("org.openrewrite.maven:rewrite-maven-plugin:%s:dry-run".formatted(MavenRunner.OPEN_REWRITE_DEFAULT_VERSION));
    }

}
