package io.arconia.cli.image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import land.oras.Registry;

import io.arconia.cli.artifact.ArtifactRegistry;
import io.arconia.cli.build.BuildArguments;
import io.arconia.cli.build.BuildImageArguments;
import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;

/**
 * {@link ImageToolRunner} implementation for building and managing container images using Buildpacks.
 */
public class BuildpacksRunner implements ImageToolRunner {

    private final BuildToolRunner buildToolRunner;
    private final OutputOptions outputOptions;

    public BuildpacksRunner(OutputOptions outputOptions, List<String> additionalParameters) {
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        Assert.notNull(additionalParameters, "additionalParameters cannot be null");
        this.outputOptions = outputOptions;
        this.buildToolRunner = BuildToolRunner.create(outputOptions, additionalParameters);
    }

    public void build(BuildArguments buildArguments) {
        Assert.notNull(buildArguments, "buildArguments cannot be null");

        if (isMultiArchBuild(buildArguments)) {
            buildMultiArch(buildArguments);
        } else {
            buildToolRunner.imageBuild(buildArguments);
        }
    }

    @Override
    public ImageBuildType getImageBuildType() {
        return ImageBuildType.BUILDPACKS;
    }

    @Override
    @Nullable
    public File getImageToolExecutable() {
        return buildToolRunner.getBuildToolExecutable();
    }

    private boolean isMultiArchBuild(BuildArguments buildArguments) {
        var imageArgs = buildArguments.buildImageArguments();
        return imageArgs != null
                && !CollectionUtils.isEmpty(imageArgs.imagePlatforms())
                && imageArgs.imagePlatforms().size() > 1;
    }

    private void buildMultiArch(BuildArguments buildArguments) {
        var imageArgs = buildArguments.buildImageArguments();
        Assert.notNull(imageArgs, "buildImageArguments must not be null for multi-arch builds");

        var imageName = imageArgs.imageName();
        var platforms = imageArgs.imagePlatforms();

        // Validate preconditions
        if (!StringUtils.hasText(imageName)) {
            throw new CliException("--image-name is required for multi-arch builds");
        }
        if (!imageArgs.publishImage()) {
            throw new CliException("--publish-image is required for multi-arch builds (images must be pushed to registry for the manifest index)");
        }

        // Validate image name includes a tag
        int tagSeparator = imageName.lastIndexOf(':');
        if (tagSeparator < 0 || tagSeparator < imageName.lastIndexOf('/')) {
            throw new CliException("Image name '%s' must include a tag (e.g. myregistry/myapp:1.0) for multi-arch builds".formatted(imageName));
        }

        outputOptions.newLine();
        outputOptions.info("Starting multi-arch build for %d platform(s): %s".formatted(
                platforms.size(), String.join(", ", platforms)));
        outputOptions.newLine();

        List<MultiArchImagePublisher.PlatformImage> builtImages = new ArrayList<>();

        for (int i = 0; i < platforms.size(); i++) {
            String platform = platforms.get(i);
            String platformImageName = ImagePlatformUtils.platformSpecificImageName(imageName, platform);

            outputOptions.info("Building image for platform %s (%d/%d)...".formatted(
                    platform, i + 1, platforms.size()));
            outputOptions.info("Image name: %s".formatted(platformImageName));
            outputOptions.info("====================================");

            try {
                // Build per-platform image with single platform and publishImage enabled
                var perPlatformImageArgs = BuildImageArguments.builder()
                        .imageName(platformImageName)
                        .builderImage(imageArgs.builderImage())
                        .runImage(imageArgs.runImage())
                        .cleanCache(imageArgs.cleanCache())
                        .publishImage(true)
                        .imagePlatforms(List.of(platform))
                        .build();

                var perPlatformBuildArgs = BuildArguments.builder()
                        .buildImageArguments(perPlatformImageArgs)
                        .clean(buildArguments.clean())
                        .skipTests(buildArguments.skipTests())
                        .build();

                buildToolRunner.imageBuild(perPlatformBuildArgs);

                builtImages.add(new MultiArchImagePublisher.PlatformImage(platformImageName, platform));

                outputOptions.newLine();
                outputOptions.info("====================================");
                outputOptions.info("Successfully built and pushed image for platform %s".formatted(platform));
                outputOptions.newLine();
            } catch (Exception ex) {
                outputOptions.newLine();
                outputOptions.info("Failed to build image for platform %s".formatted(platform));
                if (!builtImages.isEmpty()) {
                    outputOptions.info("The following platform images were already built and pushed:");
                    for (MultiArchImagePublisher.PlatformImage built : builtImages) {
                        outputOptions.info("  - %s (%s)".formatted(built.imageRef(), built.platformString()));
                    }
                }
                throw new CliException("Multi-arch build failed for platform '%s'".formatted(platform), ex);
            }
        }

        // Create and push the multi-arch manifest index
        outputOptions.info("All platform images built successfully. Creating manifest index...");
        outputOptions.newLine();

        try {
            Registry registry = ArtifactRegistry.create();
            var publisher = new MultiArchImagePublisher(registry, outputOptions);
            publisher.publish(imageName, builtImages);
        } catch (Exception ex) {
            outputOptions.info("Failed to create multi-arch manifest index");
            outputOptions.info("The following platform images were built and pushed:");
            for (MultiArchImagePublisher.PlatformImage built : builtImages) {
                outputOptions.info("  - %s (%s)".formatted(built.imageRef(), built.platformString()));
            }
            throw new CliException("Failed to create multi-arch manifest index for '%s'".formatted(imageName), ex);
        }

        outputOptions.newLine();
        outputOptions.info("Multi-arch build complete!");
        outputOptions.info("Platforms: %s".formatted(String.join(", ", platforms)));
        outputOptions.info("Image: %s".formatted(imageName));
    }

}
