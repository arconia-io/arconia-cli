package io.arconia.cli.image;

import java.io.File;

import org.jspecify.annotations.Nullable;

/**
 * Interface for running image build and management tools.
 */
public interface ImageToolRunner {

  ImageBuildType getImageBuildType();

  @Nullable
  File getImageToolExecutable();

}
