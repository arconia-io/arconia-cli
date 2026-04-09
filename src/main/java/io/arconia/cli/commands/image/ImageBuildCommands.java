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
import io.arconia.cli.image.OciRuntime;

@Component
@Command(
    name = "build",
    description = "Package a Spring Boot application as a container image."
)
public class ImageBuildCommands implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "buildpacks", description = "Package a Spring Boot application as a container image using Buildpacks.")
    public void buildpacks(
        @Option(names = "--image-name", description = "Name of the image to build (e.g. ghcr.io/arconia-io/configuration-service).") @Nullable String imageName,
        @Option(names = "--builder-image", description = "Name of the Buildpacks Builder image to use (e.g. docker.io/paketobuildpacks/builder-noble-java-tiny).") @Nullable String builderImage,
        @Option(names = "--run-image", description = "Name of the Buildpacks Run image to use (e.g. docker.io/paketobuildpacks/ubuntu-noble-run-tiny).") @Nullable String runImage,
        @Option(names = "--clean-cache", description = "Whether to clean the cache before building.") boolean cleanCache,
        @Option(names = "--publish-image", description = "Whether to publish the generated image to an OCI registry.") boolean publishImage,
        @Option(names = "--image-platform", arity = "0..*", description = "Platform(s) for the image to build (e.g. linux/amd64, linux/arm64). Specify multiple for multi-arch builds. If not specified, the platform of the host machine is used.") @Nullable List<String> imagePlatforms,
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

    @Command(name = "dockerfile", aliases = "containerfile", description = "Package a Spring Boot application as a container image using a Dockerfile or Containerfile.")
    public void dockerfile(
        @Option(names = {"-t", "--image-name"}, required = true, description = "Name of the image to build (e.g. ghcr.io/arconia-io/configuration-service).") String imageName,
        @Option(names = {"-f", "--dockerfile", "--containerfile"}, description = "The path to the Dockerfile or Containerfile to use for building the container image.") String dockerfile,
        @Option(names = "--oci-runtime", description = "The OCI runtime to use for building the container image. If unspecified, Podman is preferred when available, otherwise Docker is used.") @Nullable OciRuntime ociRuntime,
        @Mixin OutputOptions outputOptions
    ) {
        var resolvedRuntime = ociRuntime != null ? ociRuntime : OciRuntime.detect();
        outputOptions.verbose("OCI runtime: %s".formatted(resolvedRuntime.getExecutableName()));
        var dockerfileRunner = new DockerfileRunner(outputOptions, List.of(), resolvedRuntime);
        dockerfileRunner.build(imageName, dockerfile);
    }

}
