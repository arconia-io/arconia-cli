package io.arconia.cli.build;

import java.nio.file.Path;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public enum BuildTool {

    GRADLE,
    GRADLE_KOTLIN,
    MAVEN;

    @Nullable
    public static BuildTool detectFromProjectPath(Path projectPath) {
        Assert.notNull(projectPath, "projectPath cannot be null");

        if (projectPath.resolve("build.gradle").toFile().isFile()) {
            return GRADLE;
        } else if (projectPath.resolve("build.gradle.kts").toFile().isFile()) {
            return GRADLE_KOTLIN;
        } else if (projectPath.resolve("pom.xml").toFile().isFile()) {
            return MAVEN;
        }
        return null;
    }

}
