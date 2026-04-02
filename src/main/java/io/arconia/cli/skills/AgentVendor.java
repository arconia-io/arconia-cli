package io.arconia.cli.skills;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

/**
 * Known agent vendors and their conventional skill directory paths.
 * <p>
 * Acts as a CLI convenience for the {@code --agent} flag, resolving human-friendly
 * aliases to the vendor's expected skills base path. The resolved path (not the vendor
 * name) is what gets persisted in {@code skills.json} and {@code skills.lock.json}.
 *
 * @see SkillInstaller
 */
public enum AgentVendor {

    CLAUDE(".claude/skills", "claude-code", "claude"),
    VIBE(".vibe/skills", "mistral-vibe", "mistral", "vibe"),
    CONTINUE(".continue/skills", "continue"),
    BOB(".bob/skills", "bob", "ibm-bob");

    private final String skillsBasePath;
    private final List<String> aliases;

    AgentVendor(String skillsBasePath, String... aliases) {
        this.skillsBasePath = skillsBasePath;
        this.aliases = List.of(aliases);
    }

    /**
     * Returns the vendor's skills base path (e.g. {@code .claude/skills}).
     *
     * @return the skills base path
     */
    public String skillsBasePath() {
        return skillsBasePath;
    }

    /**
     * Returns the aliases accepted for this vendor on the CLI.
     *
     * @return the list of aliases
     */
    public List<String> aliases() {
        return aliases;
    }

    /**
     * Resolves a CLI alias to an {@link AgentVendor}.
     *
     * @param alias the alias to resolve (case-insensitive)
     * @return the matching vendor
     * @throws IllegalArgumentException if the alias is not recognized
     */
    public static AgentVendor resolve(String alias) {
        Assert.hasText(alias, "alias cannot be null or empty");

        String normalized = alias.strip().toLowerCase();
        for (AgentVendor vendor : values()) {
            if (vendor.aliases.contains(normalized)) {
                return vendor;
            }
        }
        throw new IllegalArgumentException(
            "Unknown agent vendor '%s'. Supported values: %s".formatted(alias, supportedAliases()));
    }

    /**
     * Resolves a list of CLI aliases to their skills base paths.
     * Duplicate paths are removed.
     *
     * @param aliases the aliases to resolve
     * @return the list of unique skills base paths
     * @throws IllegalArgumentException if any alias is not recognized
     */
    public static List<String> resolveBasePaths(List<String> aliases) {
        Assert.notNull(aliases, "aliases cannot be null");

        return aliases.stream()
            .map(AgentVendor::resolve)
            .map(AgentVendor::skillsBasePath)
            .distinct()
            .toList();
    }

    /**
     * Returns a comma-separated string of all supported aliases.
     *
     * @return the supported aliases
     */
    public static String supportedAliases() {
        return Arrays.stream(values())
            .flatMap(v -> v.aliases.stream())
            .collect(Collectors.joining(", "));
    }

}
