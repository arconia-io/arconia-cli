package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Parser for YAML frontmatter in SKILL.md files.
 * <p>
 * Extracts the {@code ---} delimited frontmatter block at the beginning of
 * a SKILL.md file and deserializes it into a typed {@link SkillFrontmatter}
 * record.
 * <p>
 * Example frontmatter:
 * <pre>
 * ---
 * name: pull-request
 * description: Creates and manages Codeberg pull requests.
 * license: Apache-2.0
 * ---
 * </pre>
 */
public final class SkillFrontmatterParser {

    /**
     * The expected filename for skill definition files.
     */
    public static final String SKILL_FILENAME = "SKILL.md";

    private static final YAMLMapper yamlMapper = YAMLMapper.builder().build();

    private SkillFrontmatterParser() {}

    /**
     * Parses the YAML frontmatter from the SKILL.md file in the given skill directory.
     *
     * @param skillDirectory the skill directory containing the SKILL.md file
     * @return the parsed frontmatter
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if SKILL.md is missing or has invalid frontmatter
     */
    public static SkillFrontmatter parseFromDirectory(Path skillDirectory) throws IOException {
        Path skillMdPath = skillDirectory.resolve(SKILL_FILENAME);
        if (!Files.exists(skillMdPath)) {
            throw new IllegalArgumentException("SKILL.md not found in directory: %s".formatted(skillDirectory));
        }

        String skillMdContent = Files.readString(skillMdPath);
        String yamlBlock = extractFrontmatterBlock(skillMdContent);

        SkillFrontmatter frontmatter;
        try {
            frontmatter = yamlMapper.readValue(yamlBlock, SkillFrontmatter.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse SKILL.md frontmatter: " + e.getMessage(), e);
        }

        // Fail early if the required 'name' field is missing or empty
        if (frontmatter.name() == null || frontmatter.name().isBlank()) {
            throw new IllegalArgumentException(
                "SKILL.md in directory '%s' is missing required frontmatter field 'name'.".formatted(skillDirectory));
        }

        return frontmatter;
    }

    /**
     * Extracts the raw YAML block between the opening and closing {@code ---} delimiters.
     */
    private static String extractFrontmatterBlock(String content) {
        String trimmed = content.stripLeading();
        if (!trimmed.startsWith("---")) {
            throw new IllegalArgumentException("SKILL.md does not start with frontmatter delimiter '---'");
        }

        int firstDelimiterEnd = trimmed.indexOf('\n');
        if (firstDelimiterEnd < 0) {
            throw new IllegalArgumentException("SKILL.md frontmatter is not properly terminated");
        }

        int secondDelimiterStart = trimmed.indexOf("\n---", firstDelimiterEnd);
        if (secondDelimiterStart < 0) {
            throw new IllegalArgumentException("SKILL.md frontmatter is missing closing '---' delimiter");
        }

        return trimmed.substring(firstDelimiterEnd + 1, secondDelimiterStart);
    }

}
