package io.arconia.cli.image;

import java.io.File;

import org.jspecify.annotations.Nullable;

public interface ImageToolRunner {

  ImageBuildType getImageBuildType();

  @Nullable
  File getImageToolExecutable();

}
