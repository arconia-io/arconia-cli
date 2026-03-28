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

import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.openrewrite.RewriteArguments;
import io.arconia.cli.openrewrite.UpdateArguments;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GradleRunner}.
 */
class GradleRunnerTests {

    @TempDir
    Path tempDir;

    GradleRunner runner;

    @BeforeEach
    void setUp() throws IOException {
        File wrapper = tempDir.resolve("gradlew").toFile();
        Files.createFile(wrapper.toPath());

        runner = new GradleRunner(createOutputOptions(), List.of(), tempDir, BuildTool.GRADLE);
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
    void buildToolIsGradle() {
        assertThat(runner.getBuildTool()).isEqualTo(BuildTool.GRADLE);
    }

    // -- constructGradleCommand tests --

    @Test
    void buildCommandDefault() {
        var arguments = BuildArguments.builder().build();
        var command = runner.constructGradleCommand("build", arguments, BootstrapMode.PROD);

        assertThat(command).contains("build", "--console=rich");
        assertThat(command).doesNotContain("clean", "-x", "test");
    }

    @Test
    void buildCommandWithClean() {
        var arguments = BuildArguments.builder().clean(true).build();
        var command = runner.constructGradleCommand("build", arguments, BootstrapMode.PROD);

        assertThat(command).containsSubsequence("clean", "build");
    }

    @Test
    void buildCommandWithSkipTests() {
        var arguments = BuildArguments.builder().skipTests(true).build();
        var command = runner.constructGradleCommand("build", arguments, BootstrapMode.TEST);

        assertThat(command).containsSubsequence("-x", "test");
    }

    @Test
    void buildCommandWithParams() {
        var runner = new GradleRunner(createOutputOptions(), List.of("--info", "--stacktrace"), tempDir, BuildTool.GRADLE);
        var command = runner.constructGradleCommand("build", BuildArguments.builder().build(), BootstrapMode.PROD);

        assertThat(command).contains("--info", "--stacktrace");
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
        var command = runner.constructGradleCommand("bootBuildImage", arguments, BootstrapMode.PROD);

        assertThat(command).contains(
            "--imageName=my-image",
            "--builder=paketobuildpacks/builder",
            "--runImage=paketobuildpacks/run",
            "--cleanCache",
            "--publishImage");
    }

    @Test
    void buildCommandNativeCompile() {
        var arguments = BuildArguments.builder().nativeBuild(true).build();
        var command = runner.constructGradleCommand("nativeCompile", arguments, BootstrapMode.PROD);

        assertThat(command).contains("nativeCompile");
    }

    // -- constructRewriteCommand tests --

    @Test
    void rewriteCommandRun() {
        var options = RewriteArguments.builder()
            .rewriteRecipeName("org.example.MyRecipe")
            .build();
        var command = runner.constructRewriteCommand(options);

        assertThat(command).contains("--init-script", "rewriteRun");
        assertThat(command).contains("-DpluginVersion=" + GradleRunner.OPEN_REWRITE_DEFAULT_VERSION);
        assertThat(command).contains("-DactiveRecipe=org.example.MyRecipe");
        assertThat(command).doesNotContain("rewriteDryRun");
    }

    @Test
    void rewriteCommandDryRun() {
        var options = RewriteArguments.builder()
            .dryRun(true)
            .rewriteRecipeName("org.example.MyRecipe")
            .build();
        var command = runner.constructRewriteCommand(options);

        assertThat(command).contains("rewriteDryRun");
        assertThat(command).doesNotContain("rewriteRun");
    }

    @Test
    void rewriteCommandWithRecipeLibrary() {
        var options = RewriteArguments.builder()
            .rewriteRecipeName("org.example.MyRecipe")
            .rewriteRecipeLibrary("org.example:my-recipes")
            .rewriteRecipeVersion("1.0.0")
            .build();
        var command = runner.constructRewriteCommand(options);

        assertThat(command).contains("-DrecipeLibrary=org.example:my-recipes");
        assertThat(command).contains("-DrecipeVersion=1.0.0");
    }

    // -- constructUpdateCommand tests --

    @Test
    void updateCommandRun() {
        var options = UpdateArguments.builder()
            .rewriteRecipeName("org.example.UpdateRecipe")
            .rewriteRecipeLibrary("org.example:update-recipes")
            .build();
        var command = runner.constructUpdateCommand(options);

        assertThat(command).contains("rewriteRun", "--no-parallel");
        assertThat(command).contains("-DactiveRecipe=org.example.UpdateRecipe");
        assertThat(command).contains("-DrecipeLibrary=org.example:update-recipes");
    }

    @Test
    void updateCommandDryRun() {
        var options = UpdateArguments.builder()
            .dryRun(true)
            .rewriteRecipeName("org.example.UpdateRecipe")
            .rewriteRecipeLibrary("org.example:update-recipes")
            .build();
        var command = runner.constructUpdateCommand(options);

        assertThat(command).contains("rewriteDryRun");
        assertThat(command).doesNotContain("rewriteRun");
    }

}
