package io.arconia.cli.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Objects;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.cli.openrewrite.OpenRewriteUtils;
import io.arconia.cli.openrewrite.UpdateOptions;
import io.arconia.cli.utils.FileUtils;
import io.arconia.cli.utils.ProcessUtils;
import io.arconia.cli.utils.SystemUtils;

public class MavenRunner implements BuildToolRunner {

    private final Path projectDir;

    public MavenRunner(Path projectDir) {
        this.projectDir = projectDir;
    }

    @Override
    public void build(BuildOptions buildOptions) {
        var command = constructMavenCommand("install", buildOptions);
        System.out.println(command.toString());
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    @Override
    public void test(BuildOptions buildOptions) {
        var command = constructMavenCommand("test", buildOptions);
        System.out.println(command.toString());
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    @Override
    public void run(BuildOptions buildOptions) {
        var command = constructMavenCommand("spring-boot:run", buildOptions);
        System.out.println(command.toString());
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    @Override
    public void imageBuild(BuildOptions buildOptions) {
        var command = constructMavenCommand("spring-boot:build-image", buildOptions);
        System.out.println(command.toString());
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    @Override
    public void update(UpdateOptions updateOptions) {
        var command = constructUpdateCommand(updateOptions);
        System.out.println(command.toString());
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    @Override
    public BuildTool getBuildTool() {
        return BuildTool.MAVEN;
    }

    @Override
    public File getBuildToolWrapper() {
        File wrapper;
        if (SystemUtils.isWindows()) {
            wrapper = new File(projectDir.toFile(), "mvnw.cmd");
        } else {
            wrapper = new File(projectDir.toFile(), "mvnw");
        }
        return wrapper;
    }

    @Override
    public File getBuildToolExecutable() {
        return FileUtils.getExecutable("mvn");
    }

    private ArrayDeque<String> constructMavenCommand(String action, BuildOptions buildOptions) {
        ArrayDeque<String> command = new ArrayDeque<>();

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

    private void addBuildImageArguments(ArrayDeque<String> command, BuildImageOptions imageOptions) {
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

    private ArrayDeque<String> constructUpdateCommand(UpdateOptions updateOptions) {
        ArrayDeque<String> command = new ArrayDeque<>();

        command.add(getBuildToolMainCommand());

        command.add("-U");
        
        if (updateOptions.dryRun()) {
            command.add("org.openrewrite.maven:rewrite-maven-plugin:%s:dry-run".formatted(Objects.requireNonNullElse(updateOptions.rewritePluginVersion(), "LATEST")));
        } else {
            command.add("org.openrewrite.maven:rewrite-maven-plugin:%s:run".formatted(Objects.requireNonNullElse(updateOptions.rewritePluginVersion(), "LATEST")));
        }

        command.add("-Drewrite.activeRecipes=" + OpenRewriteUtils.getSpringBootUpdateRecipe(updateOptions));

        command.add("-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:" + Objects.requireNonNullElse(updateOptions.springRecipesVersion(), "LATEST"));
        command.add("-Drewrite.exportDatatables=true");

        if (!CollectionUtils.isEmpty(updateOptions.params())) {
            command.addAll(updateOptions.params());
        }

        return command;
    }
    
}
