package io.arconia.cli.image;

import java.io.File;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.build.BuildOptions;
import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.core.ArconiaCliTerminal;

public class BuildpacksRunner implements ImageToolRunner {

    private final BuildToolRunner buildToolRunner;

    public BuildpacksRunner(ArconiaCliTerminal terminal) {
        Assert.notNull(terminal, "terminal cannot be null");
        this.buildToolRunner = BuildToolRunner.create(terminal);
    }

    public void build(BuildOptions buildOptions) {
        Assert.notNull(buildOptions, "buildOptions cannot be null");
        buildToolRunner.imageBuild(buildOptions);
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
