package io.arconia.cli.commands.image;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import io.arconia.cli.build.BuildArguments;
import io.arconia.cli.build.BuildImageArguments;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.commands.options.ParametersOption;
import io.arconia.cli.image.BuildpacksRunner;
import io.arconia.cli.image.DockerfileRunner;

@Component
@Command(
    name = "build",
    description = "Build a container image."
)
public class ImageBuildCommands implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "buildpacks", description = "Build a container image using Buildpacks.")
    public void buildpacks(
        @Option(names = "--image-name", description = "Name for the image to build.") @Nullable String imageName,
        @Option(names = "--builder-image", description = "Name of the Builder image to use.") @Nullable String builderImage,
        @Option(names = "--run-image", description = "Name of the Run image to use.") @Nullable String runImage,
        @Option(names = "--clean-cache", description = "Whether to clean the cache before building.") boolean cleanCache,
        @Option(names = "--publish-image", description = "Whether to publish the generated image to an OCI registry.") boolean publishImage,
        @Option(names = "--image-platform", arity = "0..*", description = "Platform(s) for the image to build (e.g. linux/amd64, linux/arm64). Specify multiple for multi-arch builds.") @Nullable List<String> imagePlatforms,
        @Option(names = "--clean", description = "Perform a clean build.") boolean clean,
        @Option(names = "--skip-tests", description = "Skip tests.") boolean skipTests,
        @Mixin OutputOptions outputOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var buildpacksRunner = new BuildpacksRunner(outputOptions, parametersOption.getParams());
        var buildArguments = BuildArguments.builder()
            .buildImageArguments(BuildImageArguments.builder()
                .imageName(imageName)
                .builderImage(builderImage)
                .runImage(runImage)
                .cleanCache(cleanCache)
                .publishImage(publishImage)
                .imagePlatforms(imagePlatforms)
                .build())
            .clean(clean)
            .skipTests(skipTests)
            .build();

        buildpacksRunner.build(buildArguments);
    }

    @Command(name = "dockerfile", description = "Build a container image using Dockerfile.")
    public void dockerfile(
        @Option(names = {"-t", "--image-name"}, required = true, description = "Name for the image to build.") String imageName,
        @Option(names = {"-f", "--dockerfile"}, description = "The path to the Dockerfile to use for building the container image.") String dockerfile,
        @Mixin OutputOptions outputOptions
    ) {
        var dockerfileRunner = new DockerfileRunner(outputOptions, List.of());
        dockerfileRunner.build(imageName, dockerfile);
    }

}
