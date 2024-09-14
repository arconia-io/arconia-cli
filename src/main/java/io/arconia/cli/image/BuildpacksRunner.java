package io.arconia.cli.image;

import java.io.File;

import io.arconia.cli.build.BuildOptions;
import io.arconia.cli.build.BuildToolRunner;

public class BuildpacksRunner implements ImageToolRunner {

    private final BuildToolRunner buildToolRunner;

    public BuildpacksRunner() {
        this.buildToolRunner = BuildToolRunner.create();
    }

    public void build(BuildOptions buildOptions) {
        buildToolRunner.imageBuild(buildOptions);
    }

    @Override
    public ImageBuildType getImageBuildType() {
        return ImageBuildType.BUILDPACKS;
    }

    @Override
    public File getImageToolExecutable() {
        return buildToolRunner.getBuildToolExecutable();
    }
  
}
