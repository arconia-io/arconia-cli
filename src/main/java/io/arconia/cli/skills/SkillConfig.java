package io.arconia.cli.skills;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The OCI Config object ({@code application/vnd.agentskills.skill.config.v1+json})
 * embedded in a Skill Artifact manifest.
 * <p>
 * Maps directly to the fields defined by the
 * <a href="https://agentskills.io/specification">Agent Skills OCI Artifact Specification</a>:
 * <ul>
 *   <li>{@code schemaVersion} — required, version of this config schema (currently {@code "1"})</li>
 *   <li>{@code name} — required, max 64 chars, lowercase letters, digits, and hyphens only</li>
 *   <li>{@code version} — optional, skill version; SHOULD follow Semantic Versioning 2.0.0</li>
 *   <li>{@code description} — optional, max 1024 chars, what the skill does and when to invoke it</li>
 *   <li>{@code license} — optional, SPDX license expression</li>
 *   <li>{@code compatibility} — optional, max 500 chars, environment requirements</li>
 *   <li>{@code allowedTools} — optional, list of pre-approved tool patterns</li>
 *   <li>{@code metadata} — optional, arbitrary string key-value pairs</li>
 * </ul>
 * <p>
 * Provides structured, queryable metadata directly accessible from the OCI manifest
 * without unpacking the content layer.
 *
 * @param schemaVersion version of this config schema, not the skill's own version (must be {@code "1"})
 * @param name skill identifier (lowercase+hyphens, max 64 chars, must match {@code SKILL.md} and OCI annotation)
 * @param version skill version sourced from the OCI tag at publish time; SHOULD follow semver 2.0.0
 * @param description what the skill does and when an agent should invoke it (max 1024 chars)
 * @param license SPDX license expression (should match {@code org.opencontainers.image.licenses} annotation)
 * @param compatibility free-text environment requirements (max 500 chars)
 * @param allowedTools list of pre-approved tool patterns (e.g. {@code Bash(git:*)})
 * @param metadata arbitrary string key-value pairs (common keys: {@code author}, {@code category})
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillConfig(
    String schemaVersion,
    String name,
    @Nullable String version,
    @Nullable String description,
    @Nullable String license,
    @Nullable String compatibility,
    @Nullable List<String> allowedTools,
    @Nullable Map<String, Object> metadata
) {

    /**
     * Current config schema version.
     */
    public static final String CURRENT_SCHEMA_VERSION = "1";

    /**
     * Regex pattern for valid skill names as defined by the Agent Skills OCI Artifact Specification.
     * <p>
     * Allowed characters: lowercase ASCII letters, digits, and hyphens.
     * Must start and end with a letter or digit. Maximum length: 64 characters.
     * Pattern: {@code ^[a-z0-9][a-z0-9-]{0,62}[a-z0-9]$}
     */
    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}[a-z0-9]$");

    public SkillConfig {
        Assert.hasText(schemaVersion, "schemaVersion cannot be null or empty");
        Assert.hasText(name, "name cannot be null or empty");
    }

    /**
     * Creates a new builder for {@link SkillConfig}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder pre-populated with the values from this config,
     * allowing selective modification.
     *
     * @return a pre-populated builder
     */
    public Builder mutate() {
        return new Builder()
            .schemaVersion(schemaVersion)
            .name(name)
            .version(version)
            .description(description)
            .license(license)
            .compatibility(compatibility)
            .allowedTools(allowedTools)
            .metadata(metadata);
    }

    /**
     * Creates a {@link SkillConfig} from parsed SKILL.md frontmatter and an explicit version.
     *
     * @param frontmatter the parsed SKILL.md frontmatter
     * @param version the skill version (typically from the OCI tag), or {@code null}
     * @return a new skill config
     * @throws IllegalArgumentException if the frontmatter has no name
     */
    public static SkillConfig fromFrontmatter(SkillFrontmatter frontmatter, @Nullable String version) {
        Assert.notNull(frontmatter, "frontmatter cannot be null");
        Assert.hasText(frontmatter.name(), "frontmatter name cannot be null or empty");
        validateName(frontmatter.name());

        List<String> allowedToolsList = null;
        if (StringUtils.hasText(frontmatter.allowedTools())) {
            // Convert a comma-separated list of allowed tools to a list of strings.
            allowedToolsList = Arrays.asList(frontmatter.allowedTools().strip().split("\\s+"));
        }

        return builder()
            .name(frontmatter.name())
            .version(version)
            .description(frontmatter.description())
            .license(frontmatter.license())
            .compatibility(frontmatter.compatibility())
            .allowedTools(allowedToolsList)
            .metadata(frontmatter.metadata())
            .build();
    }

    /**
     * Validates that a skill name conforms to the specification's naming rules.
     * <p>
     * The name must match the pattern {@code ^[a-z0-9][a-z0-9-]{0,62}[a-z0-9]$}:
     * lowercase ASCII letters, digits, and hyphens only; must start and end with
     * a letter or digit; maximum 64 characters.
     *
     * @param name the skill name to validate
     * @throws IllegalArgumentException if the name does not match the required pattern
     */
    public static void validateName(String name) {
        Assert.hasText(name, "skill name cannot be null or empty");
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                "Invalid skill name '%s'. Names must consist of lowercase letters, digits, and hyphens, start and end with a letter or digit, and be 2–64 characters long."
                    .formatted(name));
        }
    }

    /**
     * Builder for {@link SkillConfig}.
     */
    public static class Builder {

        private String schemaVersion = CURRENT_SCHEMA_VERSION;
        private String name;
        private @Nullable String version;
        private @Nullable String description;
        private @Nullable String license;
        private @Nullable String compatibility;
        private @Nullable List<String> allowedTools;
        private @Nullable Map<String, Object> metadata;

        private Builder() {}

        public Builder schemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(@Nullable String version) {
            this.version = version;
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder license(@Nullable String license) {
            this.license = license;
            return this;
        }

        public Builder compatibility(@Nullable String compatibility) {
            this.compatibility = compatibility;
            return this;
        }

        public Builder allowedTools(@Nullable List<String> allowedTools) {
            this.allowedTools = allowedTools;
            return this;
        }

        public Builder metadata(@Nullable Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public SkillConfig build() {
            return new SkillConfig(schemaVersion, name, version, description,
                license, compatibility, allowedTools, metadata);
        }

    }

}
