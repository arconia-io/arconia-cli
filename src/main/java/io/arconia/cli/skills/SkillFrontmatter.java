package io.arconia.cli.skills;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.Nullable;

/**
 * Typed representation of the YAML frontmatter in a SKILL.md file.
 * <p>
 * Maps directly to the fields defined by the
 * <a href="https://agentskills.io/specification">Agent Skills specification</a>:
 * <ul>
 *   <li>{@code name} — required, max 64 chars, lowercase letters, numbers, and hyphens only</li>
 *   <li>{@code description} — required, max 1024 chars</li>
 *   <li>{@code license} — optional, license name or reference</li>
 *   <li>{@code compatibility} — optional, max 500 chars, environment requirements</li>
 *   <li>{@code metadata} — optional, arbitrary key-value pairs</li>
 *   <li>{@code allowed-tools} — optional, space-delimited tool patterns (experimental)</li>
 * </ul>
 * <p>
 *
 * @param name the skill identifier (lowercase+hyphens, must match directory name)
 * @param description what the skill does and when to use it
 * @param license license name or reference
 * @param compatibility environment requirements
 * @param allowedTools space-delimited list of pre-approved tools
 * @param metadata arbitrary key-value pairs (may include version, author, etc.)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillFrontmatter(
        @JsonProperty("name") @Nullable String name,
        @JsonProperty("description") @Nullable String description,
        @JsonProperty("license") @Nullable String license,
        @JsonProperty("compatibility") @Nullable String compatibility,
        @JsonProperty("allowed-tools") @Nullable String allowedTools,
        @JsonProperty("metadata") @Nullable Map<String, Object> metadata
) {}
