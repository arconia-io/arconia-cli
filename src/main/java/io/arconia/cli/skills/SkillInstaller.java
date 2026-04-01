package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.springframework.util.Assert;

import land.oras.ContainerRef;
import land.oras.Manifest;
import land.oras.Registry;
import land.oras.utils.ArchiveUtils;
import land.oras.utils.SupportedCompression;

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
     * Installs a skill from an OCI artifact into the project.
     *
     * @param skillRef the OCI reference for the skill artifact
     * @param projectRoot the project root directory
     * @return the result of the installation
     * @throws IOException if the artifact cannot be pulled or extracted
     */
    public InstallResult install(SkillRef skillRef, Path projectRoot) throws IOException {
        Assert.notNull(skillRef, "skillRef cannot be null");
        Assert.notNull(projectRoot, "projectRoot cannot be null");

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

        // 3. Prepare the installation directory
        Path skillsDir = projectRoot.resolve(DEFAULT_SKILLS_PATH);
        Files.createDirectories(skillsDir);

        // 4. Pull the artifact content to a temp directory
        // Temp dir cleanup is left to the OS — avoids risky recursive deletes.
        Path tempDir = Files.createTempDirectory("arconia-skill-");
        registry.pullArtifact(containerRef, tempDir, true);

        // 5. Find the downloaded tar.gz and extract it to the skills directory
        Path tarGzFile = findTarGzFile(tempDir);
        ArchiveUtils.uncompressuntar(tarGzFile, skillsDir, SupportedCompression.GZIP.getMediaType());

        Path installPath = skillsDir.resolve(skillName);

        // 6. Update skills.json
        updateManifest(skillRef, skillName, projectRoot);

        // 7. Update skills.lock.json
        updateLockfile(resolvedRef, skillName, installPath, projectRoot);

        return new InstallResult(resolvedRef, skillName, installPath);
    }

    /**
     * Finds the first tar.gz file in the given directory.
     */
    private Path findTarGzFile(Path directory) throws IOException {
        try (var stream = Files.newDirectoryStream(directory, "*.tar.gz")) {
            for (Path entry : stream) {
                return entry;
            }
        }
        throw new IOException("No tar.gz file found in pull output directory: " + directory);
    }

    /**
     * Updates the {@code skills.json} manifest file with the newly installed skill.
     */
    private void updateManifest(SkillRef ref, String skillName, Path projectRoot) throws IOException {
        SkillsManifest manifest = SkillsManifest.load(projectRoot);

        var entry = SkillsManifest.SkillEntry.builder()
            .name(skillName)
            .source("%s/%s".formatted(ref.registry(), ref.repository()))
            .version(ref.effectiveTag())
            .build();

        manifest = manifest.addSkill(entry);
        manifest.save(projectRoot);
    }

    /**
     * Updates the {@code skills.lock.json} lock file with the exact resolved reference.
     */
    private void updateLockfile(SkillRef resolvedRef, String skillName, Path installPath,
                                Path projectRoot) throws IOException {
        SkillsLockfile lockfile = SkillsLockfile.load(projectRoot);

        var source = SkillsLockfile.Source.builder()
            .registry(resolvedRef.registry())
            .repository(resolvedRef.repository())
            .tag(resolvedRef.effectiveTag())
            .digest(resolvedRef.digest())
            .ref(resolvedRef.fullReference())
            .build();

        String relativePath = projectRoot.relativize(installPath).toString();

        var entry = SkillsLockfile.LockfileEntry.builder()
            .name(skillName)
            .path(relativePath)
            .source(source)
            .installedAt(Instant.now().toString())
            .build();

        lockfile = lockfile.addOrUpdateEntry(entry);
        lockfile.save(projectRoot);
    }

}
