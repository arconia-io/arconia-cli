package io.arconia.cli.build;

import java.nio.file.Path;

import org.springframework.lang.Nullable;

public enum BuildTool {

    GRADLE,
    GRADLE_KOTLIN,
    MAVEN;

    @Nullable
    public static BuildTool fromProjectDir(Path projectDir) {
        if (projectDir.resolve("build.gradle").toFile().exists()) {
            return GRADLE;
        } else if (projectDir.resolve("build.gradle").toFile().exists()) {
            return GRADLE_KOTLIN;
        } else if (projectDir.resolve("pom.xml").toFile().exists()) {
            return MAVEN;
        }
        return null;
    }

}
