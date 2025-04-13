package io.arconia.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.openrewrite.RecipeProvider;
import io.arconia.cli.openrewrite.RewriteOptions;
import io.arconia.cli.openrewrite.UpdateOptions;
import io.arconia.cli.utils.IoUtils;

public class MavenRunner implements BuildToolRunner {

    private static final String OPEN_REWRITE_DEFAULT_VERSION = "LATEST";

    private final ArconiaCliTerminal terminal;
    private final Path projectPath;

    public MavenRunner(ArconiaCliTerminal terminal, Path projectPath) {
        Assert.notNull(terminal, "terminal cannot be null");
        Assert.notNull(projectPath, "projectPath cannot be null");
        
        this.terminal = terminal;
        this.projectPath = projectPath;
    }

    @Override
    public void build(BuildOptions buildOptions) {
        Assert.notNull(buildOptions, "buildOptions cannot be null");
        var command = constructMavenCommand("package", buildOptions);
        call(command);
    }

    @Override
    public void test(BuildOptions buildOptions) {
        Assert.notNull(buildOptions, "buildOptions cannot be null");
        var command = constructMavenCommand("test", buildOptions);
        call(command);
    }

    @Override
    public void run(BuildOptions buildOptions) {
        Assert.notNull(buildOptions, "buildOptions cannot be null");
        var command = constructMavenCommand("spring-boot:run", buildOptions);
        call(command);
    }

    @Override
    public void imageBuild(BuildOptions buildOptions) {
        Assert.notNull(buildOptions, "buildOptions cannot be null");
        var command = constructMavenCommand("spring-boot:build-image", buildOptions);
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
        return BuildTool.MAVEN;
    }

    @Override
    public Path getProjectPath() {
        return projectPath;
    }

    @Override
    @Nullable
    public File getBuildToolWrapper() {
        return IoUtils.getBuildToolWrapper(projectPath, "mvnw", "mvnw.cmd");
    }

    @Override
    @Nullable
    public File getBuildToolExecutable() {
        var mvndExecutable = IoUtils.getExecutable("mvnd");
        if (mvndExecutable != null && mvndExecutable.isFile()) {
            return mvndExecutable;
        }
        return IoUtils.getExecutable("mvn");
    }

    @Override
    public ArconiaCliTerminal getTerminal() {
        return terminal;
    }

    private List<String> constructMavenCommand(String action, BuildOptions buildOptions) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        if (buildOptions.clean()) {
            command.add("clean");
        }

        command.add(action);

        if (buildOptions.nativeBuild()) {
            command.add("-Pnative");
        }

        if (buildOptions.skipTests()) {
            command.add("-DskipTests");
            command.add("-Dmaven.test.skip=true");
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
            command.add("-Dspring-boot.build-image.imageName=%s".formatted(imageOptions.imageName()));
        }
        if (StringUtils.hasText(imageOptions.builderImage())) {
            command.add("-Dspring-boot.build-image.builder=%s".formatted(imageOptions.builderImage()));
        }
        if (StringUtils.hasText(imageOptions.runImage())) {
            command.add("-Dspring-boot.build-image.runImage=%s".formatted(imageOptions.runImage()));
        }
        if (imageOptions.cleanCache()) {
            command.add("-Dspring-boot.build-image.cleanCache=true");
        }
        if (imageOptions.publishImage()) {
            command.add("-Dspring-boot.build-image.publish=true");
        }
    }

    private List<String> constructRewriteCommand(RewriteOptions rewriteOptions) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        command.add("-U");

        if (rewriteOptions.dryRun()) {
            command.add("org.openrewrite.maven:rewrite-maven-plugin:%s:dry-run".formatted(OPEN_REWRITE_DEFAULT_VERSION));
        } else {
            command.add("org.openrewrite.maven:rewrite-maven-plugin:%s:run".formatted(OPEN_REWRITE_DEFAULT_VERSION));
        }

        command.add("-Drewrite.activeRecipes=" + rewriteOptions.rewriteRecipeName());

        if (StringUtils.hasText(rewriteOptions.rewriteRecipeLibrary())) {
            var recipeVersion = StringUtils.hasText(rewriteOptions.rewriteRecipeVersion())
                    ? rewriteOptions.rewriteRecipeVersion()
                    : OPEN_REWRITE_DEFAULT_VERSION;
            command.add("-Drewrite.recipeArtifactCoordinates=" + "%s:%s".formatted(rewriteOptions.rewriteRecipeLibrary(), recipeVersion));
        }

        command.add("-Drewrite.exportDatatables=true");

        if (!CollectionUtils.isEmpty(rewriteOptions.params())) {
            command.addAll(rewriteOptions.params());
        }

        return command;
    }

    private List<String> constructUpdateCommand(UpdateOptions updateOptions, RecipeProvider recipeProvider) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        command.add("-U");

        if (updateOptions.dryRun()) {
            command.add("org.openrewrite.maven:rewrite-maven-plugin:%s:dry-run".formatted(OPEN_REWRITE_DEFAULT_VERSION));
        } else {
            command.add("org.openrewrite.maven:rewrite-maven-plugin:%s:run".formatted(OPEN_REWRITE_DEFAULT_VERSION));
        }

        command.add("-Drewrite.activeRecipes=" + updateOptions.rewriteRecipeName());

        switch (recipeProvider) {
            case ARCONIA -> command.add("-Drewrite.recipeArtifactCoordinates=" + "%s:%s".formatted(updateOptions.rewriteRecipeLibrary(), OPEN_REWRITE_DEFAULT_VERSION));
            case OPENREWRITE -> command.add("-Drewrite.recipeArtifactCoordinates=" + "%s:%s".formatted(updateOptions.rewriteRecipeLibrary(), OPEN_REWRITE_DEFAULT_VERSION));
        }

        command.add("-Drewrite.exportDatatables=true");

        if (!CollectionUtils.isEmpty(updateOptions.params())) {
            command.addAll(updateOptions.params());
        }

        return command;
    }

}
