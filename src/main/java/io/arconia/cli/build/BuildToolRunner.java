package io.arconia.cli.build;

import java.io.File;

import io.arconia.cli.utils.FileUtils;
import io.arconia.cli.utils.SystemUtils;

public interface BuildToolRunner {
    
    void build(BuildOptions buildOptions);

    void test(BuildOptions buildOptions);

    BuildTool getBuildTool();

    File getBuildToolWrapper();

    File getBuildToolExecutable();

    static BuildToolRunner create() {
        var projectDir = FileUtils.getProjectDir();
        var buildTool = BuildTool.fromProjectDir(projectDir);
        return switch (buildTool) {
            case GRADLE, GRADLE_KOTLIN -> new GradleRunner(projectDir, buildTool);
            case MAVEN -> new MavenRunner(projectDir);
        };
    }

}
