package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.util.Assert;

import land.oras.ContainerRef;
import land.oras.Registry;

import io.arconia.cli.utils.SemverUtils;

/**
 * Checks for and applies updates to installed agent skills.
 * <p>
 * Implements the update check workflow defined in the Agent Skills OCI Artifact
 * Specification (Section 7.4):
 * <ol>
 *   <li>List all tags for the skill repository</li>
 *   <li>Find the highest stable semver tag</li>
 *   <li>If a higher tag exists, return it as the update target</li>
 *   <li>Otherwise, check if the current tag's digest has changed (for mutable tags)</li>
 * </ol>
 */
public final class SkillUpdater {

    private final Registry registry;
    private final SkillInstaller installer;

    /**
     * Creates a new updater.
     *
     * @param registry the ORAS registry client
     * @param installer the skill installer for applying updates
     */
    public SkillUpdater(Registry registry, SkillInstaller installer) {
        Assert.notNull(registry, "registry cannot be null");
        Assert.notNull(installer, "installer cannot be null");
        this.registry = registry;
        this.installer = installer;
    }

    /**
     * The result of checking a single skill for updates.
     */
    public sealed interface UpdateCheckResult {

        /**
         * The skill is already up to date.
         *
         * @param name the skill name
         * @param tag the current tag
         */
        record UpToDate(String name, String tag) implements UpdateCheckResult {}

        /**
         * An update is available.
         *
         * @param name the skill name
         * @param currentTag the currently installed tag
         * @param newestTag the tag to update to
         * @param newestRef the full OCI reference for the update
         */
        record UpdateAvailable(String name, String currentTag, String newestTag, String newestRef) implements UpdateCheckResult {}
    }

    /**
     * Checks whether an update is available for the given lockfile entry.
     * <p>
     * Follows the update check workflow from the specification:
     * <ol>
     *   <li>Lists all tags for the skill's repository</li>
     *   <li>Finds the highest stable semver tag</li>
     *   <li>If a higher semver tag exists, reports it as an available update</li>
     *   <li>Otherwise, checks if the installed tag's digest (or {@code latest}) has changed</li>
     * </ol>
     *
     * @param entry the lockfile entry to check
     * @return the result of the update check
     */
    public UpdateCheckResult checkForUpdate(SkillsLockfile.LockfileEntry entry) {
        Assert.notNull(entry, "entry cannot be null");

        String repoBase = "%s/%s".formatted(
            entry.source().registry(),
            entry.source().repository()
        );

        // 1. List all tags
        ContainerRef repoRef = ContainerRef.parse("%s:%s".formatted(repoBase, entry.source().tag()));
        List<String> allTags = registry.getTags(repoRef).tags();

        // 2. Find the highest stable semver tag
        Optional<String> highestTag = SemverUtils.findHighestTag(allTags);

        if (highestTag.isPresent() && SemverUtils.compare(highestTag.get(), entry.source().tag()) > 0) {
            // A higher semver tag exists
            String newestTag = highestTag.get();
            String newestRef = "%s:%s".formatted(repoBase, newestTag);
            return new UpdateCheckResult.UpdateAvailable(entry.name(), entry.source().tag(), newestTag, newestRef);
        }

        // 3. No higher semver tag — check if the installed tag or 'latest' digest changed
        String checkTag = allTags.contains("latest") && !"latest".equals(entry.source().tag())
            ? "latest"
            : entry.source().tag();

        ContainerRef tagContainerRef = ContainerRef.parse("%s:%s".formatted(repoBase, checkTag));
        String currentDigest = registry.getManifest(tagContainerRef).getDigest();

        if (currentDigest.equals(entry.source().digest())) {
            return new UpdateCheckResult.UpToDate(entry.name(), entry.source().tag());
        }

        // Digest changed — update using that tag
        String newestRef = "%s:%s".formatted(repoBase, checkTag);
        return new UpdateCheckResult.UpdateAvailable(entry.name(), entry.source().tag(), checkTag, newestRef);
    }

    /**
     * Returns all tags available for a skill's repository.
     * <p>
     * Useful for verbose output during update checks.
     *
     * @param entry the lockfile entry to query
     * @return the list of tags
     */
    public List<String> listTags(SkillsLockfile.LockfileEntry entry) {
        Assert.notNull(entry, "entry cannot be null");

        String repoBase = "%s/%s".formatted(
            entry.source().registry(),
            entry.source().repository()
        );
        ContainerRef repoRef = ContainerRef.parse("%s:%s".formatted(repoBase, entry.source().tag()));
        return registry.getTags(repoRef).tags();
    }

    /**
     * Applies an update by re-installing the skill from the given reference.
     *
     * @param newestRef the OCI reference to install (e.g., {@code ghcr.io/org/skills/pull-request:2.0.0})
     * @param projectRoot the project root directory
     * @return the install result
     * @throws IOException if the artifact cannot be pulled or extracted
     */
    public SkillInstaller.InstallResult applyUpdate(String newestRef, Path projectRoot) throws IOException {
        return applyUpdate(newestRef, projectRoot, Collections.emptyList());
    }

    /**
     * Applies an update by re-installing the skill from the given reference,
     * also copying to the specified additional base paths.
     *
     * @param newestRef the OCI reference to install (e.g., {@code ghcr.io/org/skills/pull-request:2.0.0})
     * @param projectRoot the project root directory
     * @param additionalBasePaths extra base directories to copy the skill into
     * @return the install result
     * @throws IOException if the artifact cannot be pulled or extracted
     */
    public SkillInstaller.InstallResult applyUpdate(String newestRef, Path projectRoot,
                                                     List<String> additionalBasePaths) throws IOException {
        Assert.hasText(newestRef, "newestRef cannot be null or empty");
        Assert.notNull(projectRoot, "projectRoot cannot be null");
        Assert.notNull(additionalBasePaths, "additionalBasePaths cannot be null");

        SkillRef skillRef = SkillRef.parse(newestRef);
        return installer.install(skillRef, projectRoot, additionalBasePaths);
    }

}
