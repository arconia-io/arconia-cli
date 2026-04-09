package io.arconia.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.core.ProcessExecutionRequest;
import io.arconia.cli.core.ProcessExecutor;
import io.arconia.cli.openrewrite.RewriteArguments;
import io.arconia.cli.openrewrite.UpdateArguments;
import io.arconia.cli.utils.IoUtils;

/**
 * Interface for running build tools (e.g., Gradle, Maven) with common operations
 * for application development, refactoring, and publishing.
 */
public interface BuildToolRunner {

    default void call(List<String> command) {
        call(command, Map.of());
    }

    default void call(List<String> command, Map<String, String> environmentVariables) {
        Assert.notEmpty(command, "command cannot be null or empty");
        Assert.notNull(environmentVariables, "environmentVariables cannot be null");
        ProcessExecutor.execute(ProcessExecutionRequest.builder()
                        .command(command.toArray(new String[0]))
                        .targetDirectory(getProjectPath().toFile())
                        .environmentVariables(environmentVariables)
                        .outputOptions(getOutputOptions())
                        .build());
    }

    void build(BuildArguments buildArguments);

    void test(BuildArguments buildArguments);

    void dev(BuildArguments buildArguments);

    void imageBuild(BuildArguments buildArguments);

    void rewriteRun(RewriteArguments rewriteArguments);

    void rewriteDiscover();

    void update(UpdateArguments updateArguments);

    BuildTool getBuildTool();

    Path getProjectPath();

    @Nullable
    File getBuildToolWrapper();

    @Nullable
    File getBuildToolExecutable();

    default String getBuildToolMainCommand() {
        File wrapper = getBuildToolWrapper();
        if (wrapper != null && wrapper.isFile()) {
            getOutputOptions().verbose("Wrapper: %s".formatted(wrapper.getAbsolutePath()));
            return wrapper.getAbsolutePath();
        }

        File executable = getBuildToolExecutable();
        if (executable != null && executable.isFile()) {
            getOutputOptions().verbose("Executable: %s".formatted(executable.getAbsolutePath()));
            return executable.getAbsolutePath();
        }

        throw new CliException("Cannot find any wrapper or executable to run the detected build tool: %s".formatted(getBuildTool()));
    }

    OutputOptions getOutputOptions();

    static BuildToolRunner create(OutputOptions outputOptions, List<String> additionalParameters) {
        return create(IoUtils.getProjectPath(), outputOptions, additionalParameters);
    }

    static BuildToolRunner create(Path projectPath, OutputOptions outputOptions, List<String> additionalParameters) {
        Assert.notNull(projectPath, "projectPath cannot be null");
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        Assert.notNull(additionalParameters, "additionalParameters cannot be null");

        var buildTool = BuildTool.detectFromProjectPath(projectPath);

        if (buildTool == null) {
            throw new CliException("Cannot detect the build tool used for the project at %s".formatted(projectPath));
        }

        outputOptions.verbose("Project: %s".formatted(projectPath));
        outputOptions.verbose("Build tool: %s".formatted(buildTool));

        return switch (buildTool) {
            case GRADLE, GRADLE_KOTLIN -> new GradleRunner(outputOptions, additionalParameters, projectPath, buildTool);
            case MAVEN -> new MavenRunner(outputOptions, additionalParameters, projectPath);
        };
    }

}
