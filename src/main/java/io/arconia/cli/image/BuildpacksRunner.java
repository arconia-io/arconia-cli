package io.arconia.cli.image;

import java.io.File;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.build.BuildArguments;
import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.commands.options.OutputOptions;

/**
 * {@link ImageToolRunner} implementation for building and managing container images using Buildpacks.
 */
public class BuildpacksRunner implements ImageToolRunner {

    private final BuildToolRunner buildToolRunner;

    public BuildpacksRunner(OutputOptions outputOptions, List<String> additionalParameters) {
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        Assert.notNull(additionalParameters, "additionalParameters cannot be null");
        this.buildToolRunner = BuildToolRunner.create(outputOptions, additionalParameters);
    }

    public void build(BuildArguments buildArguments) {
        Assert.notNull(buildArguments, "buildArguments cannot be null");
        buildToolRunner.imageBuild(buildArguments);
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

}
