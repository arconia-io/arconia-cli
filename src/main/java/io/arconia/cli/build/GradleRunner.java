package io.arconia.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;

import org.springframework.util.CollectionUtils;

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

        if (!CollectionUtils.isEmpty(buildOptions.params())) {
            command.addAll(buildOptions.params());
        }

        return command;
    }

}
