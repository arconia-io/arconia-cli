package io.arconia.cli.image;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.cli.build.BuildOptions;
import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.core.ArconiaCliException;
import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.core.ProcessExecutor;
import io.arconia.cli.utils.IoUtils;

public class DockerfileRunner implements ImageToolRunner {

    private final ArconiaCliTerminal terminal;
    private final BuildToolRunner buildToolRunner;
    private final Path projectPath;

    public DockerfileRunner(ArconiaCliTerminal terminal) {
        Assert.notNull(terminal, "terminal cannot be null");
        this.terminal = terminal;
        this.buildToolRunner = BuildToolRunner.create(terminal);
        this.projectPath = IoUtils.getProjectPath();
    }

    public void call(List<String> command) {
        Assert.notEmpty(command, "command cannot be null or empty");
        ProcessExecutor.execute(terminal, command.toArray(new String[0]), projectPath.toFile());
    }

    public void build(String imageName, @Nullable String dockerfile) {
        Assert.hasText(imageName, "imageName cannot be null");

        terminal.verbose("☕ Building application");
        buildToolRunner.build(BuildOptions.builder().skipTests(true).build());

        terminal.newLine();

        terminal.verbose("🐳 Building container image");
        var dockerfilePath = getDockerfilePath(dockerfile);
        var action = "build";
        var command = constructImageCommand(action, imageName, dockerfilePath);
        call(command);
    }

    @Override
    public ImageBuildType getImageBuildType() {
        return ImageBuildType.DOCKERFILE;
    }

    @Override
    public File getImageToolExecutable() {
        return IoUtils.getExecutable("docker");
    }

    private Path getDockerfilePath(String dockerfile) {
        Path dockerfilePath;
        if (StringUtils.hasText(dockerfile)) {
            dockerfilePath = Path.of(dockerfile).toAbsolutePath();
            if (dockerfilePath.toFile().isFile()) {
                terminal.debug("Dockerfile: %s".formatted(dockerfilePath));
                return dockerfilePath;
            }
            throw new ArconiaCliException(terminal, "Cannot find Dockerfile at the specified path: %s".formatted(dockerfile));
        } else {
            dockerfilePath = projectPath.resolve("src/main/docker/Dockerfile");

            if (!dockerfilePath.toFile().isFile()) {
                dockerfilePath = projectPath.resolve("Dockerfile");
            }

            if (dockerfilePath.toFile().isFile()) {
                terminal.debug("Dockerfile: %s".formatted(dockerfilePath));
                return dockerfilePath;
            }

            throw new ArconiaCliException(terminal, "Cannot find Dockerfile at any of the supported locations");
        }
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

        command.add("--load");

        return command;
    }

}
