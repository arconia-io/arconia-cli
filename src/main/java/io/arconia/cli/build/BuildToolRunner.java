package io.arconia.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.core.ArconiaCliException;
import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.core.ProcessExecutor;
import io.arconia.cli.openrewrite.UpdateOptions;
import io.arconia.cli.utils.IoUtils;

public interface BuildToolRunner {

    default void call(List<String> command) {
        Assert.notEmpty(command, "command cannot be null or empty");
        ProcessExecutor.execute(getTerminal(), command.toArray(new String[0]), getProjectPath().toFile());
    }
    
    void build(BuildOptions buildOptions);

    void test(BuildOptions buildOptions);

    void run(BuildOptions buildOptions);

    void imageBuild(BuildOptions buildOptions);

    void update(UpdateOptions updateOptions);

    BuildTool getBuildTool();

    Path getProjectPath();

    @Nullable
    File getBuildToolWrapper();

    @Nullable
    File getBuildToolExecutable();

    default String getBuildToolMainCommand() {
        File wrapper = getBuildToolWrapper();
        if (wrapper != null && wrapper.isFile()) {
            getTerminal().debug("Using wrapper to run commands with %s".formatted(getBuildTool()));
            getTerminal().debug("Wrapper path: %s".formatted(wrapper.getAbsolutePath()));
            return wrapper.getAbsolutePath();
        }

        File executable = getBuildToolExecutable();
        if (executable != null && executable.isFile()) {
            getTerminal().debug("Using executable to run commands with %s".formatted(getBuildTool()));
            getTerminal().debug("Executable path: %s".formatted(executable.getAbsolutePath()));
            return executable.getAbsolutePath();
        }
        
        throw new ArconiaCliException(getTerminal(), "Cannot find any wrapper or executable to run the detected build tool: %s".formatted(getBuildTool()));
    }

    ArconiaCliTerminal getTerminal();

    static BuildToolRunner create(ArconiaCliTerminal terminal) {
        Assert.notNull(terminal, "terminal cannot be null");

        var projectPath = IoUtils.getProjectPath();
        var buildTool = BuildTool.detectFromProjectPath(projectPath);

        if (buildTool == null) {
            throw new ArconiaCliException(terminal, "Cannot detect the build tool used for the project at %s".formatted(projectPath));
        }

        terminal.debug("Project path: %s".formatted(projectPath));
        terminal.debug("Build tool: %s".formatted(buildTool));

        return switch (buildTool) {
            case GRADLE, GRADLE_KOTLIN -> new GradleRunner(terminal, projectPath, buildTool);
            case MAVEN -> new MavenRunner(terminal, projectPath);
        };
    }

}
