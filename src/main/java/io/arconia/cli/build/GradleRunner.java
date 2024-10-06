package io.arconia.cli.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.cli.utils.IoUtils;
import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.openrewrite.UpdateOptions;

public class GradleRunner implements BuildToolRunner {

    private static final String OPEN_REWRITE_DEFAULT_VERSION = "latest.release";

    private final ArconiaCliTerminal terminal;
    private final BuildTool buildTool;
    private final Path projectPath;

    public GradleRunner(ArconiaCliTerminal terminal, Path projectPath, BuildTool buildTool) {
        Assert.notNull(terminal, "terminal cannot be null");
        Assert.notNull(projectPath, "projectPath cannot be null");
        Assert.notNull(buildTool, "buildTool cannot be null");

        this.terminal = terminal;
        this.projectPath = projectPath;
        this.buildTool = buildTool;
    }

    @Override
    public void build(BuildOptions buildOptions) {
        Assert.notNull(buildOptions, "buildOptions cannot be null");
        var action = buildOptions.nativeBuild() ? "nativeBuild" : "build";
        var command = constructGradleCommand(action, buildOptions);
        call(command);
    }

    @Override
    public void test(BuildOptions buildOptions) {
        Assert.notNull(buildOptions, "buildOptions cannot be null");
        var action = buildOptions.nativeBuild() ? "nativeTest" : "test";
        var command = constructGradleCommand(action, buildOptions);
        call(command);
    }

    @Override
    public void run(BuildOptions buildOptions) {
        Assert.notNull(buildOptions, "buildOptions cannot be null");
        var action = buildOptions.nativeBuild() ? "nativeRun" : "bootRun";
        var command = constructGradleCommand(action, buildOptions);
        call(command);
    }

    @Override
    public void imageBuild(BuildOptions buildOptions) {
        Assert.notNull(buildOptions, "buildOptions cannot be null");
        var action = "bootBuildImage";
        var command = constructGradleCommand(action, buildOptions);
        call(command);
    }

    @Override
    public void rewrite(UpdateOptions updateOptions) {
        Assert.notNull(updateOptions, "updateOptions cannot be null");
        var command = constructUpdateCommand(updateOptions);
        call(command);
    }

    @Override
    public BuildTool getBuildTool() {
        return buildTool;
    }

    @Override
    public Path getProjectPath() {
        return projectPath;
    }

    @Override
    @Nullable
    public File getBuildToolWrapper() {
        return IoUtils.getBuildToolWrapper(projectPath, "gradlew", "gradlew.bat");
    }

    @Override
    @Nullable
    public File getBuildToolExecutable() {
        return IoUtils.getExecutable("gradle");
    }

    @Override
    public ArconiaCliTerminal getTerminal() {
        return terminal;
    }

    private List<String> constructGradleCommand(String action, BuildOptions buildOptions) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        if (buildOptions.clean()) {
            command.add("clean");
        }

        command.add(action);

        command.add("--console=rich");

        if (buildOptions.skipTests()) {
            command.add("-x");
            command.add("test");
        }

        if (buildOptions.buildImageOptions() != null) {
            addBuildImageArguments(command, buildOptions.buildImageOptions());
        }

        if (!CollectionUtils.isEmpty(buildOptions.params())) {
            command.addAll(buildOptions.params());
        }

        return command;
    }

    private void addBuildImageArguments(List<String> command, BuildImageOptions imageOptions) {
        if (StringUtils.hasText(imageOptions.imageName())) {
            command.add("--imageName=%s".formatted(imageOptions.imageName()));
        }
        if (StringUtils.hasText(imageOptions.builderImage())) {
            command.add("--builder=%s".formatted(imageOptions.builderImage()));
        }
        if (StringUtils.hasText(imageOptions.runImage())) {
            command.add("--runImage=%s".formatted(imageOptions.runImage()));
        }
        if (imageOptions.cleanCache()) {
            command.add("--cleanCache");
        }
        if (imageOptions.publishImage()) {
            command.add("--publishImage");
        }
    }

    private List<String> constructUpdateCommand(UpdateOptions updateOptions) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        command.add("--init-script");

        try {
            command.add(IoUtils.copyFileToTemp("openrewrite/init.gradle").toFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (updateOptions.dryRun()) {
            command.add("rewriteDryRun");
        } else {
            command.add("rewriteRun");
        }

        command.add("-PpluginVersion=" + OPEN_REWRITE_DEFAULT_VERSION);

        command.add("-DactiveRecipe=" + updateOptions.rewriteRecipeName());
        command.add("-PrecipeLibrary=" + updateOptions.rewriteRecipeLibrary());
        command.add("-PrecipeVersion=" + OPEN_REWRITE_DEFAULT_VERSION);

        if (!CollectionUtils.isEmpty(updateOptions.params())) {
            command.addAll(updateOptions.params());
        }

        return command;
    }

}
