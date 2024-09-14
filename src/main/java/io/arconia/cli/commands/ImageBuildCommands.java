package io.arconia.cli.commands;

import java.util.Arrays;
import java.util.List;

import org.springframework.shell.command.CommandRegistration.OptionArity;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import io.arconia.cli.build.BuildImageOptions;
import io.arconia.cli.build.BuildOptions;
import io.arconia.cli.image.BuildpacksRunner;
import io.arconia.cli.image.DockerfileRunner;

@Command(command = "image build", group = "Image")
public class ImageBuildCommands {

    @Command(command = "buildpacks", description = "Build a container image using Buildpacks.")
    public void buildpacks(
        @Option(description = "Name for the image to build.") String imageName,
        @Option(description = "Name of the Builder image to use.") String builderImage,
        @Option(description = "Name of the Run image to use.") String runImage,
        @Option(description = "Whether to clean the cache before building.") boolean cleanCache,
        @Option(description = "Whether to publish the generated image to an OCI registry.") boolean publishImage,

        @Option(description = "Perform a clean build.") boolean clean,
        @Option(description = "Skip tests") boolean skipTests,
        @Option(required = false, description = "Additional build parameters", arity = OptionArity.ZERO_OR_MORE) String[] params
    ) {
        var buildpacksRunner = new BuildpacksRunner();
        var buildOptions = BuildOptions.builder()
            .buildImageOptions(BuildImageOptions.builder()
                .imageName(imageName)
                .builderImage(builderImage)
                .runImage(runImage)
                .cleanCache(cleanCache)
                .publishImage(publishImage)
                .build())
            .clean(clean)
            .skipTests(skipTests)
            .params(params != null ? Arrays.asList(params) : List.of())
            .build();
        buildpacksRunner.build(buildOptions);
    }

    @Command(command = "dockerfile", description = "Build a container image using Dockerfile.")
    public void dockerfile(
        @Option(description = "Name for the image to build.", shortNames = 't') String imageName,
        @Option(required = false, description = "The path to the Dockerfile to use for building the container image.", shortNames = 'f') String dockerfile
    ) {
        var dockerfileRunner = new DockerfileRunner();
        dockerfileRunner.build(imageName, dockerfile);
    }
    
}
