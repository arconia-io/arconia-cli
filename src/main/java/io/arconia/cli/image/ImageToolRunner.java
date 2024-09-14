package io.arconia.cli.image;

import java.io.File;

public interface ImageToolRunner {

  ImageBuildType getImageBuildType();

  File getImageToolExecutable();

}
