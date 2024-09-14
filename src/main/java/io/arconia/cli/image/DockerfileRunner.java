package io.arconia.cli.image;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;

import org.springframework.util.StringUtils;

import io.arconia.cli.utils.FileUtils;
import io.arconia.cli.utils.ProcessUtils;

public class DockerfileRunner {

    private final Path projectDir;

    public DockerfileRunner() {
        this.projectDir = FileUtils.getProjectDir();
    }
  
    public void build(String imageReference, String dockerfile) { 
        var dockerfilePath = getDockerfilePath(dockerfile);
        var command = constructImageCommand("build", imageReference, dockerfilePath);
        ProcessUtils.executeProcess(command.toArray(new String[0]), projectDir.toFile());
    }

    public ImageBuildType getImageBuildType() {
        return ImageBuildType.DOCKERFILE;
    }

    public File getImageBuildExecutable() {
        return FileUtils.getExecutable("docker");
    }

    private Path getDockerfilePath(String dockerfile) {
        Path dockerfilePath;
        if (StringUtils.hasText(dockerfile)) {
            dockerfilePath = Paths.get(dockerfile).toAbsolutePath();
            if (!dockerfilePath.toFile().exists()) {
                throw new RuntimeException("Cannot find a Dockerfile from " + dockerfile);
            }
        } else {
            dockerfilePath = projectDir.resolve("src/main/docker/Dockerfile");
            if (!dockerfilePath.toFile().exists()) {
                dockerfilePath = projectDir.resolve("Dockerfile");
            }
            
            if (!dockerfilePath.toFile().exists()) {
                throw new RuntimeException("Cannot find a Dockerfile in any of the default locations");
            }
        }
        return dockerfilePath;
    }

    private ArrayDeque<String> constructImageCommand(String action, String imageReference, Path dockerfilePath) {
        ArrayDeque<String> command = new ArrayDeque<>();

        command.add(getImageBuildExecutable().getAbsolutePath());

        command.add(action);

        command.add("--tag");
        command.add(imageReference);
        
        command.add("--file");
        command.add(dockerfilePath.toFile().getAbsolutePath());

        command.add(projectDir.toFile().getAbsolutePath());

        command.add("--load");

        return command;
    }

}