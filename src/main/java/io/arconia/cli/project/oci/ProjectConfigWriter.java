package io.arconia.cli.project.oci;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.util.Assert;

import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * Writes a {@link ProjectConfig} to a YAML file.
 */
public final class ProjectConfigWriter {

    private static final YAMLMapper yamlMapper = YAMLMapper.builder()
            .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .build();

    private ProjectConfigWriter() {}

    /**
     * Writes the given {@link ProjectConfig} as a YAML file to the specified directory.
     */
    public static void writeToDirectory(ProjectConfig config, Path directory, boolean force) throws IOException {
        Assert.notNull(config, "config cannot be null");
        Assert.notNull(directory, "directory cannot be null");

        Path configFile = directory.resolve(ProjectConfigParser.CONFIG_FILE_NAME);

        if (Files.exists(configFile) && !force) {
            throw new IllegalStateException(
                "%s already exists in directory: %s. Use --force to overwrite.".formatted(
                    ProjectConfigParser.CONFIG_FILE_NAME, directory));
        }

        String yaml = yamlMapper.writeValueAsString(config);
        Files.writeString(configFile, yaml);
    }

}
