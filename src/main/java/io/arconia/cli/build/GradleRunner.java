package io.arconia.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.cli.utils.FileUtils;
import io.arconia.cli.utils.ProcessUtils;
import io.arconia.cli.utils.SystemUtils;

public class GradleRunner implements BuildToolRunner {

    private final BuildTool buildTool;
    private final Path projectDir;

    public GradleRunner(Path projectDir, BuildTool buildTool) {
        this.projectDir = projectDir;
        this.buildTool = buildTool;
    }

    @Override
    public void build(BuildOptions buildOptions) {
        var command = constructGradleCommand("build", "nativeBuild", buildOptions);
        System.out.println(command.toString());
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    @Override
    public void test(BuildOptions buildOptions) {
        var command = constructGradleCommand("test", "nativeTest", buildOptions);
        System.out.println(command.toString());
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    @Override
    public void run(BuildOptions buildOptions) {
        var command = constructGradleCommand("bootRun", "nativeRun", buildOptions);
        System.out.println(command.toString());
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    @Override
    public void imageBuild(BuildOptions buildOptions) {
        var command = constructGradleCommand("bootBuildImage", "bootBuildImage", buildOptions);
        System.out.println(command.toString());
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    @Override
    public BuildTool getBuildTool() {
        return buildTool;
    }

    @Override
    public File getBuildToolWrapper() {
        File wrapper;
        if (SystemUtils.isWindows()) {
            wrapper = new File(projectDir.toFile(), "gradlew.bat");
        } else {
            wrapper = new File(projectDir.toFile(), "gradlew");
        }
        return wrapper;
    }

    @Override
    public File getBuildToolExecutable() {
        return FileUtils.getExecutable("gradle");
    }

    private ArrayDeque<String> constructGradleCommand(String action, String nativeAction, BuildOptions buildOptions) {
        ArrayDeque<String> command = new ArrayDeque<>();

        File wrapper = getBuildToolWrapper();
        if (wrapper.exists()) {
            command.add(wrapper.getAbsolutePath());
        } else {
            command.add(getBuildToolExecutable().getAbsolutePath());
        }

        if (buildOptions.clean()) {
            command.add("clean");
        }

        if (buildOptions.nativeBuild()) {
            command.add(nativeAction);
        } else {
            command.add(action);
        }

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

    private void addBuildImageArguments(ArrayDeque<String> command, BuildImageOptions imageOptions) {
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

}
