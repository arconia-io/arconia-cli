package io.arconia.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;

import org.springframework.util.CollectionUtils;

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
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    @Override
    public void test(BuildOptions buildOptions) {
        var command = constructMavenCommand("test", buildOptions);
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    @Override
    public void run(BuildOptions buildOptions) {
        var command = constructMavenCommand("spring-boot:run", buildOptions);
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

        File wrapper = getBuildToolWrapper();
        if (wrapper.exists()) {
            command.add(wrapper.getAbsolutePath());
        } else {
            command.add(getBuildToolExecutable().getAbsolutePath());
        }

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

        if (!CollectionUtils.isEmpty(buildOptions.params())) {
            command.addAll(buildOptions.params());
        }

        return command;
    }
    
}
