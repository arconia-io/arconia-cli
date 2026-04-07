package io.arconia.cli.project.oci;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Parser for the project configuration file.
 */
public final class ProjectConfigParser {

    public static final String CONFIG_FILE_NAME = "project.yml";

    private static final YAMLMapper yamlMapper = YAMLMapper.builder().build();

    private ProjectConfigParser() {}

    /**
     * Reads the project configuration file from the given directory and parses it into a {@link ProjectConfig} object.
     */
    public static ProjectConfig parseFromDirectory(Path projectDirectory) throws IOException {
        Path configFile = projectDirectory.resolve(CONFIG_FILE_NAME);
        if (!Files.exists(configFile)) {
            throw new IllegalArgumentException("%s not found in directory: %s".formatted(CONFIG_FILE_NAME, projectDirectory));
        }

        String configFileContent = Files.readString(configFile);

        try {
            return yamlMapper.readValue(configFileContent, ProjectConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse %s: %s".formatted(CONFIG_FILE_NAME, e.getMessage()), e);
        }
    }

}
