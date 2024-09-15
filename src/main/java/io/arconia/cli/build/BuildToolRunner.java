package io.arconia.cli.build;

import java.io.File;

import io.arconia.cli.openrewrite.UpdateOptions;
import io.arconia.cli.utils.FileUtils;

public interface BuildToolRunner {
    
    void build(BuildOptions buildOptions);

    void test(BuildOptions buildOptions);

    void run(BuildOptions buildOptions);

    void imageBuild(BuildOptions buildOptions);

    void update(UpdateOptions updateOptions);

    BuildTool getBuildTool();

    File getBuildToolWrapper();

    File getBuildToolExecutable();

    default String getBuildToolMainCommand() {
        File wrapper = getBuildToolWrapper();
        if (wrapper.exists()) {
            return wrapper.getAbsolutePath();
        } else {
            return getBuildToolExecutable().getAbsolutePath();
        }
    }

    static BuildToolRunner create() {
        var projectDir = FileUtils.getProjectDir();
        var buildTool = BuildTool.fromProjectDir(projectDir);
        return switch (buildTool) {
            case GRADLE, GRADLE_KOTLIN -> new GradleRunner(projectDir, buildTool);
            case MAVEN -> new MavenRunner(projectDir);
        };
    }

}
