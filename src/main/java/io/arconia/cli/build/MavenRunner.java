package io.arconia.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.openrewrite.RewriteArguments;
import io.arconia.cli.openrewrite.UpdateArguments;
import io.arconia.cli.utils.IoUtils;

/**
 * {@link BuildToolRunner} implementation for Maven projects.
 */
public class MavenRunner implements BuildToolRunner {

    static final String OPEN_REWRITE_DEFAULT_VERSION = "LATEST";

    private final OutputOptions outputOptions;
    private final List<String> additionalParameters;
    private final Path projectPath;

    public MavenRunner(OutputOptions outputOptions, List<String> additionalParameters, Path projectPath) {
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        Assert.notNull(additionalParameters, "additionalParameters cannot be null");
        Assert.notNull(projectPath, "projectPath cannot be null");

        this.outputOptions = outputOptions;
        this.additionalParameters = additionalParameters;
        this.projectPath = projectPath;
    }

    @Override
    public void build(BuildArguments buildArguments) {
        Assert.notNull(buildArguments, "buildArguments cannot be null");
        var command = constructMavenCommand("clean", buildArguments);
        call(command);
    }

    @Override
    public void test(BuildArguments buildArguments) {
        Assert.notNull(buildArguments, "buildArguments cannot be null");
        var command = constructMavenCommand("clean", buildArguments);
        call(command, Map.of(BootstrapMode.ENV_VAR_KEY, BootstrapMode.TEST.toString()));
    }

    @Override
    public void dev(BuildArguments buildArguments) {
        Assert.notNull(buildArguments, "buildArguments cannot be null");
        var action = buildArguments.testClasspath() ? "spring-boot:test-run" : "spring-boot:run";
        var command = constructMavenCommand(action, buildArguments);
        call(command, Map.of(BootstrapMode.ENV_VAR_KEY, BootstrapMode.DEV.toString()));
    }

    @Override
    public void imageBuild(BuildArguments buildArguments) {
        Assert.notNull(buildArguments, "buildArguments cannot be null");
        var command = constructMavenCommand("clean", buildArguments);
        call(command);
    }

    @Override
    public void rewriteRun(RewriteArguments rewriteArguments) {
        Assert.notNull(rewriteArguments, "rewriteArguments cannot be null");
        var command = constructRewriteRunCommand(rewriteArguments);
        call(command);
    }

    @Override
    public void rewriteDiscover() {
        var command = constructRewriteDiscoverCommand();
        call(command);
    }

    @Override
    public void update(UpdateArguments updateArguments) {
        Assert.notNull(updateArguments, "updateArguments cannot be null");
        var command = constructUpdateCommand(updateArguments);
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
    public OutputOptions getOutputOptions() {
        return outputOptions;
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

    // Package-private for testability

    List<String> constructMavenCommand(String action, BuildArguments buildArguments) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        if (buildArguments.clean()) {
            command.add("clean");
        }

        command.add(action);

        if (buildArguments.nativeBuild()) {
            command.add("-Pnative");
        }

        if (buildArguments.skipTests() || action.equals("spring-boot:build-image")) {
            command.add("-DskipTests");
            command.add("-Dmaven.test.skip=true");
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
            command.add("-Dspring-boot.build-image.imageName=%s".formatted(imageArguments.imageName()));
        }
        if (StringUtils.hasText(imageArguments.builderImage())) {
            command.add("-Dspring-boot.build-image.builder=%s".formatted(imageArguments.builderImage()));
        }
        if (StringUtils.hasText(imageArguments.runImage())) {
            command.add("-Dspring-boot.build-image.runImage=%s".formatted(imageArguments.runImage()));
        }
        if (imageArguments.cleanCache()) {
            command.add("-Dspring-boot.build-image.cleanCache=true");
        }
        if (imageArguments.publishImage()) {
            command.add("-Dspring-boot.build-image.publish=true");
        }
        if (!CollectionUtils.isEmpty(imageArguments.imagePlatforms()) && imageArguments.imagePlatforms().size() == 1) {
            command.add("-Dspring-boot.build-image.imagePlatform=%s".formatted(imageArguments.imagePlatforms().getFirst()));
        }
    }

    List<String> constructRewriteRunCommand(RewriteArguments rewriteArguments) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        command.add("-U");

        if (rewriteArguments.dryRun()) {
            command.add("org.openrewrite.maven:rewrite-maven-plugin:%s:dry-run".formatted(OPEN_REWRITE_DEFAULT_VERSION));
        } else {
            command.add("org.openrewrite.maven:rewrite-maven-plugin:%s:run".formatted(OPEN_REWRITE_DEFAULT_VERSION));
        }

        command.add("-Drewrite.activeRecipes=" + rewriteArguments.rewriteRecipeName());

        List<String> coordinates = new ArrayList<>();

        String arconiaVersion = "LATEST";
        String coreRewriteVersion = OPEN_REWRITE_DEFAULT_VERSION;

        coordinates.add("io.arconia.migrations:rewrite-arconia:" + arconiaVersion);
        coordinates.add("io.arconia.migrations:rewrite-docling:" + arconiaVersion);
        coordinates.add("io.arconia.migrations:rewrite-spring:" + arconiaVersion);
        coordinates.add("io.arconia.migrations:rewrite-test:" + arconiaVersion);

        coordinates.add("org.openrewrite:rewrite-java:" + coreRewriteVersion);
        coordinates.add("org.openrewrite.recipe:rewrite-java-dependencies:" + coreRewriteVersion);

        if (StringUtils.hasText(rewriteArguments.rewriteRecipeLibrary())) {
            var recipeVersion = StringUtils.hasText(rewriteArguments.rewriteRecipeVersion())
                    ? rewriteArguments.rewriteRecipeVersion()
                    : OPEN_REWRITE_DEFAULT_VERSION;
            coordinates.add("%s:%s".formatted(rewriteArguments.rewriteRecipeLibrary(), recipeVersion));
        }

        command.add("-Drewrite.recipeArtifactCoordinates=" + String.join(",", coordinates));

        if (rewriteArguments.rewriteConfigFile() != null) {
            command.add("-Drewrite.configLocation=" + rewriteArguments.rewriteConfigFile().toAbsolutePath());
        }

        command.add("-Drewrite.exportDatatables=true");

        if (!CollectionUtils.isEmpty(additionalParameters)) {
            command.addAll(additionalParameters);
        }

        return command;
    }

    List<String> constructRewriteDiscoverCommand() {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        command.add("-U");

        command.add("org.openrewrite.maven:rewrite-maven-plugin:%s:discover".formatted(OPEN_REWRITE_DEFAULT_VERSION));

        List<String> coordinates = new ArrayList<>();

        String arconiaVersion = "LATEST";
        String coreRewriteVersion = OPEN_REWRITE_DEFAULT_VERSION;

        coordinates.add("io.arconia.migrations:rewrite-arconia:" + arconiaVersion);
        coordinates.add("io.arconia.migrations:rewrite-docling:" + arconiaVersion);
        coordinates.add("io.arconia.migrations:rewrite-spring:" + arconiaVersion);
        coordinates.add("io.arconia.migrations:rewrite-test:" + arconiaVersion);

        coordinates.add("org.openrewrite:rewrite-java:" + coreRewriteVersion);
        coordinates.add("org.openrewrite.recipe:rewrite-java-dependencies:" + coreRewriteVersion);

        command.add("-Drewrite.recipeArtifactCoordinates=" + String.join(",", coordinates));

        if (!CollectionUtils.isEmpty(additionalParameters)) {
            command.addAll(additionalParameters);
        }

        return command;
    }

    List<String> constructUpdateCommand(UpdateArguments updateArguments) {
        List<String> command = new ArrayList<>();

        command.add(getBuildToolMainCommand());

        command.add("-U");

        if (updateArguments.dryRun()) {
            command.add("org.openrewrite.maven:rewrite-maven-plugin:%s:dry-run".formatted(OPEN_REWRITE_DEFAULT_VERSION));
        } else {
            command.add("org.openrewrite.maven:rewrite-maven-plugin:%s:run".formatted(OPEN_REWRITE_DEFAULT_VERSION));
        }

        command.add("-Drewrite.activeRecipes=" + updateArguments.rewriteRecipeName());

        command.add("-Drewrite.recipeArtifactCoordinates=" + "%s:%s".formatted(updateArguments.rewriteRecipeLibrary(), OPEN_REWRITE_DEFAULT_VERSION));

        command.add("-Drewrite.exportDatatables=true");

        if (!CollectionUtils.isEmpty(additionalParameters)) {
            command.addAll(additionalParameters);
        }

        return command;
    }

}
