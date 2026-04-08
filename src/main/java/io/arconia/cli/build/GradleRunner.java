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

import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.openrewrite.RewriteArguments;
import io.arconia.cli.openrewrite.UpdateArguments;
import io.arconia.cli.utils.IoUtils;

/**
 * {@link BuildToolRunner} implementation for Gradle projects.
 */
public class GradleRunner implements BuildToolRunner {

    static final String OPEN_REWRITE_DEFAULT_VERSION = "latest.release";

    private final OutputOptions outputOptions;
    private final List<String> additionalParameters;
    private final Path projectPath;
    private final BuildTool buildTool;

    public GradleRunner(OutputOptions outputOptions, List<String> additionalParameters, Path projectPath, BuildTool buildTool) {
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        Assert.notNull(additionalParameters, "additionalParameters cannot be null");
        Assert.notNull(projectPath, "projectPath cannot be null");
        Assert.notNull(buildTool, "buildTool cannot be null");

        this.outputOptions = outputOptions;
        this.additionalParameters = additionalParameters;
        this.projectPath = projectPath;
        this.buildTool = buildTool;
    }

    @Override
    public void build(BuildArguments buildArguments) {
        Assert.notNull(buildArguments, "buildArguments cannot be null");
        var action = buildArguments.nativeBuild() ? "nativeBuild" : "build";
        call(constructGradleCommand(action, buildArguments, BootstrapMode.PROD));
    }

    @Override
    public void test(BuildArguments buildArguments) {
        Assert.notNull(buildArguments, "buildArguments cannot be null");
        var action = buildArguments.nativeBuild() ? "nativeTest" : "test";
        call(constructGradleCommand(action, buildArguments, BootstrapMode.TEST));
    }

    @Override
    public void dev(BuildArguments buildArguments) {
        Assert.notNull(buildArguments, "buildArguments cannot be null");
        var action = buildArguments.testClasspath() ? "bootTestRun" : "bootRun";
        call(constructGradleCommand(action, buildArguments, BootstrapMode.DEV));
    }

    @Override
    public void imageBuild(BuildArguments buildArguments) {
        Assert.notNull(buildArguments, "buildArguments cannot be null");
        call(constructGradleCommand("bootBuildImage", buildArguments, BootstrapMode.PROD));
    }

    @Override
    public void rewriteRun(RewriteArguments rewriteArguments) {
        Assert.notNull(rewriteArguments, "rewriteArguments cannot be null");
        call(constructRewriteRunCommand(rewriteArguments));
    }

    @Override
    public void rewriteDiscover() {
        call(constructRewriteDiscoverCommand());
    }

    @Override
    public void update(UpdateArguments updateArguments) {
        Assert.notNull(updateArguments, "updateArguments cannot be null");
        call(constructUpdateCommand(updateArguments));
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
    public OutputOptions getOutputOptions() {
        return outputOptions;
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

    // Package-private for testability

    List<String> constructGradleCommand(String action, BuildArguments buildArguments, BootstrapMode bootstrapMode) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        if (buildArguments.clean()) {
            command.add("clean");
        }

        command.add(action);

        command.add("--console=rich");

        if (buildArguments.skipTests()) {
            command.add("-x");
            command.add("test");
        }

        if (buildArguments.offline()) {
            command.add("--offline");
        }

        if (buildArguments.buildImageArguments() != null) {
            addBuildImageArguments(command, buildArguments.buildImageArguments());
        }

        if (!CollectionUtils.isEmpty(additionalParameters)) {
            command.addAll(additionalParameters);
        }

        return command;
    }

    private void addBuildImageArguments(List<String> command, BuildImageArguments imageArguments) {
        if (StringUtils.hasText(imageArguments.imageName())) {
            command.add("--imageName=%s".formatted(imageArguments.imageName()));
        }
        if (StringUtils.hasText(imageArguments.builderImage())) {
            command.add("--builder=%s".formatted(imageArguments.builderImage()));
        }
        if (StringUtils.hasText(imageArguments.runImage())) {
            command.add("--runImage=%s".formatted(imageArguments.runImage()));
        }
        if (imageArguments.cleanCache()) {
            command.add("--cleanCache");
        }
        if (imageArguments.publishImage()) {
            command.add("--publishImage");
        }
    }

    List<String> constructRewriteRunCommand(RewriteArguments rewriteArguments) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        command.add("--init-script");

        try {
            command.add(IoUtils.copyFileToTemp("openrewrite/init-rewrite.gradle").toFile().getAbsolutePath());
        } catch (IOException ex) {
            throw new CliException("Failed to copy OpenRewrite init script", ex);
        }

        if (rewriteArguments.dryRun()) {
            command.add("rewriteDryRun");
        } else {
            command.add("rewriteRun");
        }

        command.add("-DpluginVersion=" + OPEN_REWRITE_DEFAULT_VERSION);

        command.add("-DactiveRecipe=" + rewriteArguments.rewriteRecipeName());

        if (StringUtils.hasText(rewriteArguments.rewriteRecipeLibrary())) {
            var recipeVersion = StringUtils.hasText(rewriteArguments.rewriteRecipeVersion())
                    ? rewriteArguments.rewriteRecipeVersion()
                    : OPEN_REWRITE_DEFAULT_VERSION;
            command.add("-DrecipeLibrary=" + rewriteArguments.rewriteRecipeLibrary());
            command.add("-DrecipeVersion=" + recipeVersion);
        }

        if (rewriteArguments.rewriteConfigFile() != null) {
            command.add("-DrewriteConfigFile=" + rewriteArguments.rewriteConfigFile().toAbsolutePath());
        }

        if (!CollectionUtils.isEmpty(additionalParameters)) {
            command.addAll(additionalParameters);
        }

        return command;
    }

    List<String> constructRewriteDiscoverCommand() {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        command.add("--init-script");

        try {
            command.add(IoUtils.copyFileToTemp("openrewrite/init-rewrite.gradle").toFile().getAbsolutePath());
        } catch (IOException ex) {
            throw new CliException("Failed to copy OpenRewrite init script", ex);
        }

        command.add("rewriteDiscover");

        if (!CollectionUtils.isEmpty(additionalParameters)) {
            command.addAll(additionalParameters);
        }

        return command;
    }

    List<String> constructUpdateCommand(UpdateArguments updateArguments) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        command.add("--init-script");

        try {
            command.add(IoUtils.copyFileToTemp("openrewrite/init-rewrite.gradle").toFile().getAbsolutePath());
        } catch (IOException ex) {
            throw new CliException("Failed to copy OpenRewrite init script", ex);
        }

        if (updateArguments.dryRun()) {
            command.add("rewriteDryRun");
        } else {
            command.add("rewriteRun");
        }

        command.add("-DpluginVersion=" + OPEN_REWRITE_DEFAULT_VERSION);

        command.add("-DactiveRecipe=" + updateArguments.rewriteRecipeName());

        command.add("-DrecipeLibrary=" + updateArguments.rewriteRecipeLibrary());

        command.add("--no-parallel");

        if (!CollectionUtils.isEmpty(additionalParameters)) {
            command.addAll(additionalParameters);
        }

        return command;
    }

}
