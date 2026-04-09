package io.arconia.cli.image;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.cli.build.BuildArguments;
import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.core.ProcessExecutionRequest;
import io.arconia.cli.core.ProcessExecutor;
import io.arconia.cli.utils.IoUtils;

/**
 * {@link ImageToolRunner} implementation for building and managing container images using Dockerfiles.
 */
public class DockerfileRunner implements ImageToolRunner {

    private final OutputOptions outputOptions;
    private final BuildToolRunner buildToolRunner;
    private final Path projectPath;
    private final OciRuntime ociRuntime;

    public DockerfileRunner(OutputOptions outputOptions, List<String> additionalParameters, OciRuntime ociRuntime) {
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        Assert.notNull(additionalParameters, "additionalParameters cannot be null");
        Assert.notNull(ociRuntime, "ociRuntime cannot be null");
        this.outputOptions = outputOptions;
        this.buildToolRunner = BuildToolRunner.create(outputOptions, additionalParameters);
        this.projectPath = IoUtils.getProjectPath();
        this.ociRuntime = ociRuntime;
    }

    public void call(List<String> command) {
        Assert.notEmpty(command, "command cannot be null or empty");
        ProcessExecutor.execute(ProcessExecutionRequest.builder()
                        .command(command.toArray(new String[0]))
                        .targetDirectory(projectPath.toFile())
                        .outputOptions(outputOptions)
                        .build());
    }

    public void build(String imageName, @Nullable String dockerfile) {
        Assert.hasText(imageName, "imageName cannot be null");

        outputOptions.info("Building application...");
        outputOptions.newLine();
        buildToolRunner.build(BuildArguments.builder().skipTests(true).build());

        outputOptions.newLine();
        outputOptions.info("====================================");
        outputOptions.newLine();

        outputOptions.info("Building container image...");
        outputOptions.newLine();
        var dockerfilePath = getDockerfilePath(dockerfile);
        var command = constructImageCommand("build", imageName, dockerfilePath);
        call(command);
    }

    @Override
    public ImageBuildType getImageBuildType() {
        return ImageBuildType.DOCKERFILE;
    }

    @Override
    public File getImageToolExecutable() {
        return IoUtils.getExecutable(ociRuntime.getExecutableName());
    }

    private Path getDockerfilePath(String dockerfile) {
        if (StringUtils.hasText(dockerfile)) {
            Path dockerfilePath = Path.of(dockerfile).toAbsolutePath();
            if (dockerfilePath.toFile().isFile()) {
                outputOptions.verbose("Dockerfile/Containerfile: %s".formatted(dockerfilePath));
                return dockerfilePath;
            }
            throw new CliException("Cannot find Dockerfile or Containerfile at the specified path: %s".formatted(dockerfile));
        }

        // Search for Dockerfile or Containerfile in supported locations
        List<Path> candidates = List.of(
            projectPath.resolve("src/main/docker/Dockerfile"),
            projectPath.resolve("src/main/docker/Containerfile"),
            projectPath.resolve("src/main/podman/Dockerfile"),
            projectPath.resolve("src/main/podman/Containerfile"),
            projectPath.resolve("Dockerfile"),
            projectPath.resolve("Containerfile")
        );

        for (Path candidate : candidates) {
            if (candidate.toFile().isFile()) {
                outputOptions.verbose("Dockerfile/Containerfile: %s".formatted(candidate));
                return candidate;
            }
        }

        throw new CliException("Cannot find Dockerfile or Containerfile at any of the supported locations");
    }

    private List<String> constructImageCommand(String action, String imageName, Path dockerfilePath) {
        List<String> command = new ArrayList<>();

        command.add(getImageToolExecutable().getAbsolutePath());

        command.add(action);

        command.add("--tag");
        command.add(imageName);

        command.add("--file");
        command.add(dockerfilePath.toFile().getAbsolutePath());

        command.add(projectPath.toFile().getAbsolutePath());

        return command;
    }

}
