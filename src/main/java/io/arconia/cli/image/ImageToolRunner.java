package io.arconia.cli.image;

import java.io.File;

import org.springframework.lang.Nullable;

public interface ImageToolRunner {

  ImageBuildType getImageBuildType();

  @Nullable
  File getImageToolExecutable();

}
