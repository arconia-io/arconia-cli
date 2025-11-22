package io.arconia.cli.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.cli.utils.IoUtils;
import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.openrewrite.RecipeProvider;
import io.arconia.cli.openrewrite.RewriteOptions;
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
        var action = switch(buildOptions.trait()) {
            case NATIVE_BUILD -> "nativeCompile";
            default -> "build";
        };
        var command = constructGradleCommand(action, buildOptions);
        call(command);
    }

    @Override
    public void test(BuildOptions buildOptions) {
        Assert.notNull(buildOptions, "buildOptions cannot be null");
        var action = switch(buildOptions.trait()) {
            case NATIVE_BUILD -> "nativeTest";
            default -> "test";
        };
        var command = constructGradleCommand(action, buildOptions);
        call(command);
    }

    @Override
    public void dev(BuildOptions buildOptions) {
        Assert.notNull(buildOptions, "buildOptions cannot be null");
        var action = switch(buildOptions.trait()) {
            case NATIVE_BUILD -> "nativeRun";
            case TEST_CLASSPATH -> "bootTestRun";
            default -> "bootRun";
        };
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
    public void rewrite(RewriteOptions rewriteOptions) {
        Assert.notNull(rewriteOptions, "rewriteOptions cannot be null");
        var command = constructRewriteCommand(rewriteOptions);
        call(command);
    }

    @Override
    public void update(UpdateOptions updateOptions, RecipeProvider recipeProvider) {
        Assert.notNull(updateOptions, "updateOptions cannot be null");
        Assert.notNull(recipeProvider, "recipeProvider cannot be null");
        var command = constructUpdateCommand(updateOptions, recipeProvider);
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

    private List<String> constructRewriteCommand(RewriteOptions rewriteOptions) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        command.add("--init-script");

        try {
            command.add(IoUtils.copyFileToTemp("openrewrite/init-generic.gradle").toFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (rewriteOptions.dryRun()) {
            command.add("rewriteDryRun");
        } else {
            command.add("rewriteRun");
        }

        command.add("-DpluginVersion=" + OPEN_REWRITE_DEFAULT_VERSION);

        command.add("-DactiveRecipe=" + rewriteOptions.rewriteRecipeName());

        if (StringUtils.hasText(rewriteOptions.rewriteRecipeLibrary())) {
            var recipeVersion = StringUtils.hasText(rewriteOptions.rewriteRecipeVersion())
                    ? rewriteOptions.rewriteRecipeVersion()
                    : OPEN_REWRITE_DEFAULT_VERSION;
            command.add("-DrecipeLibrary=" + rewriteOptions.rewriteRecipeLibrary());
            command.add("-DrecipeVersion=" + recipeVersion);
        }

        if (!CollectionUtils.isEmpty(rewriteOptions.params())) {
            command.addAll(rewriteOptions.params());
        }

        return command;
    }

    private List<String> constructUpdateCommand(UpdateOptions updateOptions, RecipeProvider recipeProvider) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        command.add("--init-script");

        String initGradleFilePath = switch(recipeProvider) {
            case ARCONIA -> "openrewrite/init-arconia.gradle";
            case OPENREWRITE -> "openrewrite/init-openrewrite.gradle";
        };

        try {
            command.add(IoUtils.copyFileToTemp(initGradleFilePath).toFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (updateOptions.dryRun()) {
            command.add("rewriteDryRun");
        } else {
            command.add("rewriteRun");
        }

        command.add("-DpluginVersion=" + OPEN_REWRITE_DEFAULT_VERSION);

        command.add("-DactiveRecipe=" + updateOptions.rewriteRecipeName());

        command.add("-DrecipeLibrary=" + updateOptions.rewriteRecipeLibrary());
        
        switch (recipeProvider) {
            case ARCONIA -> command.add("-DbomVersion=" + OPEN_REWRITE_DEFAULT_VERSION);
            case OPENREWRITE -> command.add("-DbomVersion=" + OPEN_REWRITE_DEFAULT_VERSION);
        }

        command.add("--no-parallel");

        if (!CollectionUtils.isEmpty(updateOptions.params())) {
            command.addAll(updateOptions.params());
        }

        return command;
    }

}
