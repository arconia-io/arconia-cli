package io.arconia.cli.skills;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Formats skill and catalog data into aligned tabular text for CLI output.
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
     *
     * @param lockfile the lockfile containing installed skills
     * @return the formatted table lines (header + one row per skill)
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
        lines.add(rowFormat.formatted("NAME", "VERSION", "SOURCE", "PATH"));

        for (SkillsLockfile.LockfileEntry entry : lockfile.skills()) {
            String source = entry.source().registry() + "/" + entry.source().repository();
            lines.add(rowFormat.formatted(
                entry.name(),
                entry.source().tag(),
                source,
                entry.path()
            ));
        }

        return lines;
    }

    /**
     * Formats verbose details for a single lockfile entry.
     *
     * @param entry the lockfile entry
     * @param nameWidth the column width for alignment
     * @return the formatted verbose lines (digest, install date)
     */
    public static List<String> formatInstalledSkillVerbose(SkillsLockfile.LockfileEntry entry, int nameWidth) {
        Assert.notNull(entry, "entry cannot be null");

        List<String> lines = new ArrayList<>();
        lines.add("  %-" + nameWidth + "s  Digest: %s".formatted("", entry.source().digest()));
        lines.add("  %-" + nameWidth + "s  Installed: %s".formatted("", entry.installedAt()));
        return lines;
    }

    // ---- Catalog skills table (from SkillSummary) ----

    /**
     * Formats a list of skill summaries into an aligned table.
     *
     * @param skills the skill summaries to format
     * @return the formatted table lines (header + one row per skill)
     */
    public static List<String> formatCatalogSkills(List<SkillCatalogService.SkillSummary> skills) {
        Assert.notNull(skills, "skills cannot be null");

        List<String> lines = new ArrayList<>();

        // Compute column widths
        int nameWidth = "NAME".length();
        int versionWidth = "VERSION".length();
        for (SkillCatalogService.SkillSummary skill : skills) {
            nameWidth = Math.max(nameWidth, skill.name().length());
            if (skill.version() != null) {
                versionWidth = Math.max(versionWidth, skill.version().length());
            }
        }

        String rowFormat = "  %-" + nameWidth + "s  %-" + versionWidth + "s  %s";
        lines.add(rowFormat.formatted("NAME", "VERSION", "DESCRIPTION"));

        for (SkillCatalogService.SkillSummary skill : skills) {
            String version = skill.version() != null ? skill.version() : "";
            String description = skill.description() != null ? skill.description() : "";
            lines.add(rowFormat.formatted(skill.name(), version, description));
        }

        return lines;
    }

    /**
     * Formats verbose details for a single skill summary.
     *
     * @param skill the skill summary
     * @param nameWidth the column width for alignment
     * @return the formatted verbose lines (ref, digest)
     */
    public static List<String> formatCatalogSkillVerbose(SkillCatalogService.SkillSummary skill, int nameWidth) {
        Assert.notNull(skill, "skill cannot be null");

        List<String> lines = new ArrayList<>();
        if (skill.ref() != null) {
            lines.add("  %-" + nameWidth + "s  Ref: %s".formatted("", skill.ref()));
        }
        if (skill.digest() != null) {
            lines.add("  %-" + nameWidth + "s  Digest: %s".formatted("", skill.digest()));
        }
        return lines;
    }

    /**
     * Computes the maximum name column width from a list of skill summaries.
     *
     * @param skills the skill summaries
     * @return the maximum name width (at least as wide as "NAME")
     */
    public static int computeNameWidth(List<SkillCatalogService.SkillSummary> skills) {
        Assert.notNull(skills, "skills cannot be null");

        int nameWidth = "NAME".length();
        for (SkillCatalogService.SkillSummary skill : skills) {
            nameWidth = Math.max(nameWidth, skill.name().length());
        }
        return nameWidth;
    }

}
