package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import land.oras.ContainerRef;
import land.oras.Manifest;
import land.oras.Registry;
import land.oras.utils.ArchiveUtils;
import land.oras.utils.SupportedCompression;

import io.arconia.cli.utils.IoUtils;

/**
 * Installs agent skills from OCI artifacts into a project's skills directory.
 * <p>
 * Implements the installation workflow defined in the Agent Skills OCI Artifact Specification:
 * <ol>
 *   <li>Resolves the OCI reference to a manifest digest</li>
 *   <li>Pulls the skill artifact content layer</li>
 *   <li>Extracts the tar.gz archive to the skills directory</li>
 *   <li>Updates {@code skills.json} and {@code skills.lock.json}</li>
 * </ol>
 */
public final class SkillInstaller {

    /**
     * Default skills directory path relative to the project root.
     */
    public static final String DEFAULT_SKILLS_PATH = ".agents/skills";

    private final Registry registry;

    /**
     * Creates a new installer using the given registry client.
     *
     * @param registry the ORAS registry client
     */
    public SkillInstaller(Registry registry) {
        Assert.notNull(registry, "registry cannot be null");
        this.registry = registry;
    }

    /**
     * Result of a successful skill installation.
     *
     * @param ref the skill reference with resolved digest
     * @param skillName the installed skill name
     * @param installPath the path where the skill was extracted
     */
    public record InstallResult(
        SkillRef ref,
        String skillName,
        Path installPath
    ) {
        public InstallResult {
            Assert.notNull(ref, "ref cannot be null");
            Assert.hasText(skillName, "skillName cannot be empty");
            Assert.notNull(installPath, "installPath cannot be null");
        }
    }

    /**
     * Installs a skill from an OCI artifact into the project's default skills directory.
     *
     * @param skillRef the OCI reference for the skill artifact
     * @param projectRoot the project root directory
     * @return the result of the installation
     * @throws IOException if the artifact cannot be pulled or extracted
     */
    public InstallResult install(SkillRef skillRef, Path projectRoot) throws IOException {
        return install(skillRef, projectRoot, List.of());
    }

    /**
     * Installs a skill from an OCI artifact into the project.
     * <p>
     * The skill is always extracted to the default {@code .agents/skills} directory.
     * If additional base paths are specified, a per-skill symbolic link is created in each
     * vendor directory pointing back to the primary install location. The OCI artifact is
     * only fetched once.
     *
     * @param skillRef the OCI reference for the skill artifact
     * @param projectRoot the project root directory
     * @param additionalBasePaths extra base directories to copy the skill into (e.g. {@code .claude/skills})
     * @return the result of the installation
     * @throws IOException if the artifact cannot be pulled or extracted
     */
    public InstallResult install(SkillRef skillRef, Path projectRoot, List<String> additionalBasePaths) throws IOException {
        Assert.notNull(skillRef, "skillRef cannot be null");
        Assert.notNull(projectRoot, "projectRoot cannot be null");
        Assert.notNull(additionalBasePaths, "additionalBasePaths cannot be null");

        // 1. Resolve the manifest to get the digest
        ContainerRef containerRef = skillRef.toContainerRef();
        Manifest manifest;
        try {
            manifest = registry.getManifest(containerRef);
        }
        catch (Exception e) {
            throw new IOException("Cannot resolve skill artifact '%s'. Verify the reference and tag exist in the registry.".formatted(
                skillRef.fullTagReference()), e);
        }
        String digest = manifest.getDigest();
        SkillRef resolvedRef = skillRef.mutate().digest(digest).build();

        // 2. Use the repository slug as the skill name (last segment of the OCI ref path)
        String skillName = skillRef.skillName();

        // 3. Prepare the primary installation directory
        Path skillsDir = projectRoot.resolve(DEFAULT_SKILLS_PATH);
        Files.createDirectories(skillsDir);

        // 4. Pull the artifact content to a project-local temp directory, then extract
        Path tempDir = IoUtils.createTempDirectory(projectRoot);
        try {
            registry.pullArtifact(containerRef, tempDir, true);

            // 5. Find the downloaded tar.gz and extract it to the skills directory
            Path tarGzFile = IoUtils.findTarGzFile(tempDir);
            ArchiveUtils.uncompressuntar(tarGzFile, skillsDir, SupportedCompression.GZIP.getMediaType());
        }
        finally {
            IoUtils.deleteTempDirectory(tempDir);
        }

        Path installPath = skillsDir.resolve(skillName);

        // 6. Create symlinks in additional vendor directories
        List<String> resolvedAdditionalPaths = new ArrayList<>();
        for (String basePath : additionalBasePaths) {
            Path vendorInstallPath = projectRoot.resolve(basePath).resolve(skillName).normalize();
            validateInsideProjectRoot(vendorInstallPath, projectRoot, basePath);
            IoUtils.createSkillSymlink(vendorInstallPath, installPath);
            resolvedAdditionalPaths.add(projectRoot.relativize(vendorInstallPath).toString());
        }

        // 7. Update skills.json
        updateManifest(skillRef, skillName, additionalBasePaths, projectRoot);

        // 8. Update skills.lock.json
        updateLockfile(resolvedRef, skillName, installPath, resolvedAdditionalPaths, projectRoot);

        return new InstallResult(resolvedRef, skillName, installPath);
    }

    /**
     * Validates that the resolved path is inside the project root.
     * Prevents path traversal via malicious {@code additionalBasePaths} in {@code skills.json}.
     *
     * @param resolvedPath the resolved absolute path to validate
     * @param projectRoot the project root directory
     * @param basePath the original base path (for error messages)
     */
    private static void validateInsideProjectRoot(Path resolvedPath, Path projectRoot, String basePath) {
        Path normalizedResolved = resolvedPath.toAbsolutePath().normalize();
        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        if (!normalizedResolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException(
                "Refusing to create skill entry at '%s': additionalBasePath '%s' resolves outside the project root '%s'"
                    .formatted(normalizedResolved, basePath, normalizedRoot));
        }
    }

    /**
     * Updates the {@code skills.json} manifest file with the newly installed skill.
     */
    private void updateManifest(SkillRef ref, String skillName, List<String> additionalBasePaths,
                                Path projectRoot) throws IOException {
        SkillsManifest manifest = SkillsManifest.load(projectRoot);

        var entryBuilder = SkillsManifest.SkillEntry.builder()
            .name(skillName)
            .source("%s/%s".formatted(ref.registry(), ref.repository()))
            .version(ref.effectiveTag());

        if (!additionalBasePaths.isEmpty()) {
            entryBuilder.additionalBasePaths(additionalBasePaths);
        }

        manifest = manifest.addSkill(entryBuilder.build());
        manifest.save(projectRoot);
    }

    /**
     * Updates the {@code skills.lock.json} lock file with the exact resolved reference.
     */
    private void updateLockfile(SkillRef resolvedRef, String skillName, Path installPath,
                                List<String> additionalPaths, Path projectRoot) throws IOException {
        SkillsLockfile lockfile = SkillsLockfile.load(projectRoot);

        var source = SkillsLockfile.Source.builder()
            .registry(resolvedRef.registry())
            .repository(resolvedRef.repository())
            .tag(resolvedRef.effectiveTag())
            .digest(resolvedRef.digest())
            .ref(resolvedRef.fullReference())
            .build();

        String relativePath = projectRoot.relativize(installPath).toString();

        var entryBuilder = SkillsLockfile.LockfileEntry.builder()
            .name(skillName)
            .path(relativePath)
            .source(source)
            .installedAt(Instant.now().toString());

        if (!additionalPaths.isEmpty()) {
            entryBuilder.additionalPaths(additionalPaths);
        }

        lockfile = lockfile.addOrUpdateEntry(entryBuilder.build());
        lockfile.save(projectRoot);
    }

}
