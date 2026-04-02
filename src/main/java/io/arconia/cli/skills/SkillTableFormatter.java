package io.arconia.cli.skills;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Formats skill and collection data into aligned tabular text for CLI output.
 * <p>
 * Produces a list of formatted lines (header + rows) that the calling command
 * can print via its own output mechanism, keeping presentation logic separate
 * from both business logic and terminal I/O.
 */
public final class SkillTableFormatter {

    private SkillTableFormatter() {}

    // ---- Installed skills table (from lockfile) ----

    /**
     * Formats the installed skills from a lockfile into an aligned table.
     * <p>
     * When a skill has additional paths (vendor-specific copies), they are shown
     * on continuation lines aligned under the PATHS column.
     *
     * @param lockfile the lockfile containing installed skills
     * @return the formatted table lines (header + rows per skill)
     */
    public static List<String> formatInstalledSkills(SkillsLockfile lockfile) {
        Assert.notNull(lockfile, "lockfile cannot be null");

        List<String> lines = new ArrayList<>();

        // Compute column widths
        int nameWidth = "NAME".length();
        int versionWidth = "VERSION".length();
        int sourceWidth = "SOURCE".length();
        for (SkillsLockfile.LockfileEntry entry : lockfile.skills()) {
            nameWidth = Math.max(nameWidth, entry.name().length());
            versionWidth = Math.max(versionWidth, entry.source().tag().length());
            String source = entry.source().registry() + "/" + entry.source().repository();
            sourceWidth = Math.max(sourceWidth, source.length());
        }

        String rowFormat = "  %-" + nameWidth + "s  %-" + versionWidth + "s  %-" + sourceWidth + "s  %s";
        lines.add(rowFormat.formatted("NAME", "VERSION", "SOURCE", "PATHS"));

        // Continuation line prefix: same width as NAME + VERSION + SOURCE columns, but empty
        String continuationPrefix = "  " + " ".repeat(nameWidth) + "  " + " ".repeat(versionWidth) + "  " + " ".repeat(sourceWidth) + "  ";

        for (SkillsLockfile.LockfileEntry entry : lockfile.skills()) {
            String source = entry.source().registry() + "/" + entry.source().repository();

            // First line: skill info + primary path
            lines.add(rowFormat.formatted(
                entry.name(),
                entry.source().tag(),
                source,
                entry.path()
            ));

            // Additional paths on continuation lines
            if (entry.additionalPaths() != null) {
                for (String additionalPath : entry.additionalPaths()) {
                    lines.add(continuationPrefix + additionalPath);
                }
            }
        }

        return lines;
    }

    // ---- Collection skills table (from SkillSummary) ----

    /**
     * Formats a list of skill summaries into an aligned table.
     *
     * @param skills the skill summaries to format
     * @return the formatted table lines (header + one row per skill)
     */
    public static List<String> formatCollectionSkills(List<SkillCollectionService.SkillSummary> skills) {
        Assert.notNull(skills, "skills cannot be null");

        List<String> lines = new ArrayList<>();

        // Compute column widths
        int nameWidth = "NAME".length();
        int versionWidth = "VERSION".length();
        for (SkillCollectionService.SkillSummary skill : skills) {
            nameWidth = Math.max(nameWidth, skill.name().length());
            if (skill.version() != null) {
                versionWidth = Math.max(versionWidth, skill.version().length());
            }
        }

        String rowFormat = "  %-" + nameWidth + "s  %-" + versionWidth + "s  %s";
        lines.add(rowFormat.formatted("NAME", "VERSION", "DESCRIPTION"));

        for (SkillCollectionService.SkillSummary skill : skills) {
            String version = skill.version() != null ? skill.version() : "";
            String description = skill.description() != null ? skill.description() : "";
            lines.add(rowFormat.formatted(skill.name(), version, description));
        }

        return lines;
    }

    /**
     * Computes the maximum name column width from a list of skill summaries.
     *
     * @param skills the skill summaries
     * @return the maximum name width (at least as wide as "NAME")
     */
    public static int computeNameWidth(List<SkillCollectionService.SkillSummary> skills) {
        Assert.notNull(skills, "skills cannot be null");

        int nameWidth = "NAME".length();
        for (SkillCollectionService.SkillSummary skill : skills) {
            nameWidth = Math.max(nameWidth, skill.name().length());
        }
        return nameWidth;
    }

}
