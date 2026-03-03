package io.arconia.cli.commands;

import org.springframework.stereotype.Component;

import io.arconia.cli.build.BuildImageOptions;
import io.arconia.cli.build.BuildOptions;
import io.arconia.cli.core.ArconiaCliTerminal;
import io.arconia.cli.image.BuildpacksRunner;
import io.arconia.cli.image.DockerfileRunner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Component
@Command(
    name = "build",
    description = "Build a container image."
)
public class ImageBuildCommands implements Runnable {

    @Spec
    CommandSpec spec;

    private final ArconiaCliTerminal terminal;

    public ImageBuildCommands(ArconiaCliTerminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "buildpacks", description = "Build a container image using Buildpacks.")
    public void buildpacks(
        @Option(names = "--image-name", description = "Name for the image to build.") String imageName,
        @Option(names = "--builder-image", description = "Name of the Builder image to use.") String builderImage,
        @Option(names = "--run-image", description = "Name of the Run image to use.") String runImage,
        @Option(names = "--clean-cache", description = "Whether to clean the cache before building.") boolean cleanCache,
        @Option(names = "--publish-image", description = "Whether to publish the generated image to an OCI registry.") boolean publishImage,
        @Option(names = "--clean", description = "Perform a clean build.") boolean clean,
        @Option(names = "--skip-tests", description = "Skip tests.") boolean skipTests,
        @Mixin TroubleshootOptions troubleshootOptions,
        @Mixin ParametersOption parametersOption
    ) {
        var buildpacksRunner = new BuildpacksRunner(terminal, troubleshootOptions);
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
            .params(parametersOption.getParams())
            .build();

        buildpacksRunner.build(buildOptions);
    }

    @Command(name = "dockerfile", description = "Build a container image using Dockerfile.")
    public void dockerfile(
        @Option(names = {"-t", "--image-name"}, required = true, description = "Name for the image to build.") String imageName,
        @Option(names = {"-f", "--dockerfile"}, description = "The path to the Dockerfile to use for building the container image.") String dockerfile,
        @Mixin TroubleshootOptions troubleshootOptions
    ) {
        var dockerfileRunner = new DockerfileRunner(terminal, troubleshootOptions);
        dockerfileRunner.build(imageName, dockerfile);
    }

}
