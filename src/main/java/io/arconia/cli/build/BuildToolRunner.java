package io.arconia.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.commands.TroubleshootOptions;
import io.arconia.cli.core.ArconiaCliException;
import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.core.ProcessExecutor;
import io.arconia.cli.openrewrite.RewriteOptions;
import io.arconia.cli.openrewrite.UpdateOptions;
import io.arconia.cli.utils.IoUtils;

public interface BuildToolRunner {

    default void call(List<String> command) {
        Assert.notEmpty(command, "command cannot be null or empty");
        ProcessExecutor.execute(getTerminal(), getTroubleshootOptions(), command.toArray(new String[0]), getProjectPath().toFile());
    }

    void build(BuildOptions buildOptions);

    void test(BuildOptions buildOptions);

    void dev(BuildOptions buildOptions);

    void imageBuild(BuildOptions buildOptions);

    void rewrite(RewriteOptions rewriteOptions);

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
            getTerminal().verbose(getTroubleshootOptions().isVerbose(), "Wrapper: %s".formatted(wrapper.getAbsolutePath()));
            return wrapper.getAbsolutePath();
        }

        File executable = getBuildToolExecutable();
        if (executable != null && executable.isFile()) {
            getTerminal().verbose(getTroubleshootOptions().isVerbose(), "Executable: %s".formatted(executable.getAbsolutePath()));
            return executable.getAbsolutePath();
        }

        throw new ArconiaCliException("Cannot find any wrapper or executable to run the detected build tool: %s".formatted(getBuildTool()));
    }

    ArconiaCliTerminal getTerminal();

    TroubleshootOptions getTroubleshootOptions();

    static BuildToolRunner create(ArconiaCliTerminal terminal, TroubleshootOptions troubleshootOptions) {
        Assert.notNull(terminal, "terminal cannot be null");
        Assert.notNull(troubleshootOptions, "troubleshootOptions cannot be null");

        var projectPath = IoUtils.getProjectPath();
        var buildTool = BuildTool.detectFromProjectPath(projectPath);

        if (buildTool == null) {
            throw new ArconiaCliException("Cannot detect the build tool used for the project at %s".formatted(projectPath));
        }

        terminal.verbose(troubleshootOptions.isVerbose(), "Project: %s".formatted(projectPath));
        terminal.verbose(troubleshootOptions.isVerbose(), "Build tool: %s".formatted(buildTool));

        return switch (buildTool) {
            case GRADLE, GRADLE_KOTLIN -> new GradleRunner(terminal, troubleshootOptions, projectPath, buildTool);
            case MAVEN -> new MavenRunner(terminal, troubleshootOptions, projectPath);
        };
    }

}
