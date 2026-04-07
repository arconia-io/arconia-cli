package io.arconia.cli.commands.skills;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import land.oras.Registry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.artifact.ArtifactRegistry;
import io.arconia.cli.skills.AgentVendor;
import io.arconia.cli.skills.ArtifactPublishReport;
import io.arconia.cli.skills.SkillBatchPublisher;
import io.arconia.cli.skills.SkillBatchPublisher.BatchEntryResult;
import io.arconia.cli.skills.SkillCollectionService;
import io.arconia.cli.skills.SkillFrontmatterParser;
import io.arconia.cli.skills.SkillInstaller;
import io.arconia.cli.skills.SkillPublisher;
import io.arconia.cli.skills.SkillRef;
import io.arconia.cli.skills.SkillTableFormatter;
import io.arconia.cli.skills.SkillUpdater;
import io.arconia.cli.skills.SkillUpdater.UpdateCheckResult;
import io.arconia.cli.skills.SkillsLockfile;
import io.arconia.cli.skills.SkillsManifest;
import io.arconia.cli.utils.DateTimeUtils;
import io.arconia.cli.utils.IoUtils;

@Component
@Command(
    name = "skills",
    description = "Install and manage agent skills.",
    subcommands = {
        SkillsCollectionCommands.class
    }
)
public class SkillsCommands implements Runnable {

    private final Terminal terminal;

    public SkillsCommands(Terminal terminal) {
        this.terminal = terminal;
    }

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "add", description = "Add agent skills to the project. Use --ref for direct OCI references, or --name to look up skills from a registered collection.")
    public void add(
        @Option(names = {"--ref"}, arity = "1..*", description = "The OCI artifact reference for the agent skill (e.g. ghcr.io/org/repo/skill:1.0.0).") List<String> skillRefs,
        @Option(names = {"--name"}, arity = "1..*", description = "The skill name to look up from a registered collection.") List<String> skillNames,
        @Option(names = {"--agent"}, arity = "1..*", description = "Additional agent vendors to also install the skill for (e.g. claude, vibe, continue, bob).") List<String> agents,
        @Option(names = {"--collection"}, description = "The registered collection alias to search. If omitted, searches all registered collections.") String collectionAlias,
        @Option(names = {"--project-dir"}, description = "The project root directory. Defaults to the current working directory.") String projectDir,
        @Mixin OutputOptions outputOptions
    ) {
        boolean hasRefs = skillRefs != null && !skillRefs.isEmpty();
        boolean hasNames = skillNames != null && !skillNames.isEmpty();

        if (!hasRefs && !hasNames) {
            throw new CliException("At least one of --ref or --name must be provided.");
        }

        // Resolve --agent aliases to base paths
        List<String> additionalBasePaths = resolveAgentBasePaths(agents);

        Path projectRoot = IoUtils.getProjectPath(projectDir);
        Registry ociRegistry = ArtifactRegistry.create();
        SkillInstaller installer = new SkillInstaller(ociRegistry);

        // 1. Install by direct OCI reference
        if (hasRefs) {
            for (String ref : skillRefs) {
                installByRef(ref, installer, projectRoot, additionalBasePaths, outputOptions);
            }
        }

        // 2. Install by name from collection
        if (hasNames) {
            SkillCollectionService collectionService = new SkillCollectionService(ociRegistry);
            for (String name : skillNames) {
                installFromCollection(name, collectionAlias, collectionService, installer, projectRoot, additionalBasePaths, outputOptions);
            }
        }
    }

    /**
     * Resolves agent vendor aliases to their skills base paths.
     *
     * @param agents the list of agent aliases from the CLI, or {@code null}
     * @return the resolved base paths, or an empty list if no agents were specified
     */
    private List<String> resolveAgentBasePaths(@Nullable List<String> agents) {
        if (agents == null || agents.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return AgentVendor.resolveBasePaths(agents);
        }
        catch (IllegalArgumentException e) {
            throw new CliException(e.getMessage(), e);
        }
    }

    /**
     * Installs a skill by direct OCI reference.
     */
    private void installByRef(String ref, SkillInstaller installer, Path projectRoot,
                              List<String> additionalBasePaths, OutputOptions outputOptions) {
        try {
            outputOptions.info("Resolving skill: %s".formatted(ref));

            SkillRef skillRef = SkillRef.parse(ref);
            SkillInstaller.InstallResult result = installer.install(skillRef, projectRoot, additionalBasePaths);

            outputOptions.info("Installed skill '%s' → %s".formatted(
                result.skillName(),
                result.installPath()
            ));
            if (!additionalBasePaths.isEmpty()) {
                outputOptions.info("  Also linked from: %s".formatted(String.join(", ", additionalBasePaths)));
            }
            outputOptions.verbose("Digest: %s".formatted(result.ref().digest()));
        }
        catch (IOException e) {
            throw new CliException("Failed to install skill '%s': %s".formatted(ref, e.getMessage()), e);
        }
        catch (IllegalArgumentException e) {
            throw new CliException("Invalid skill reference '%s': %s".formatted(ref, e.getMessage()), e);
        }
    }

    /**
     * Resolves a skill name from registered collections and installs it.
     */
    private void installFromCollection(String skillName, String collectionAlias,
                                     SkillCollectionService collectionService, SkillInstaller installer,
                                     Path projectRoot, List<String> additionalBasePaths, OutputOptions outputOptions) {
        try {
            SkillCollectionService.CollectionSkillMatch match = collectionService.resolveSkillFromCollection(skillName, collectionAlias);
            String ref = match.skill().ref();

            if (ref == null || ref.isBlank()) {
                throw new CliException("Skill '%s' in collection '%s' has no OCI reference.".formatted(skillName, match.collectionName()));
            }

            outputOptions.verbose("Resolved skill '%s' from collection '%s' → %s".formatted(skillName, match.collectionName(), ref));

            installByRef(ref, installer, projectRoot, additionalBasePaths, outputOptions);
        }
        catch (IOException e) {
            throw new CliException("Failed to resolve skill '%s' from collection: %s".formatted(skillName, e.getMessage()), e);
        }
        catch (IllegalArgumentException e) {
            throw new CliException(e.getMessage(), e);
        }
    }

    @Command(name = "install", description = "Install all agent skills declared in skills.json.")
    public void install(
        @Option(names = {"--project-dir"}, description = "The project root directory. Defaults to the current working directory.") String projectDir,
        @Mixin OutputOptions outputOptions
    ) {
        Path projectRoot = IoUtils.getProjectPath(projectDir);

        try {
            SkillsManifest manifest = SkillsManifest.load(projectRoot);

            if (manifest.skills().isEmpty()) {
                outputOptions.info("No skills declared in %s.".formatted(SkillsManifest.FILENAME));
                return;
            }

            outputOptions.info("Installing %d skill(s) from %s...".formatted(manifest.skills().size(), SkillsManifest.FILENAME));

            Registry ociRegistry = ArtifactRegistry.create();
            SkillInstaller installer = new SkillInstaller(ociRegistry);

            for (SkillsManifest.SkillEntry entry : manifest.skills()) {
                String reference = entry.version() != null
                        ? "%s:%s".formatted(entry.source(), entry.version())
                        : entry.source();

                // Use additionalBasePaths from the manifest entry
                List<String> additionalBasePaths = entry.additionalBasePaths() != null
                        ? entry.additionalBasePaths()
                        : Collections.emptyList();

                installByRef(reference, installer, projectRoot, additionalBasePaths, outputOptions);
            }
        }
        catch (IOException e) {
            throw new CliException("Failed to read %s: %s".formatted(SkillsManifest.FILENAME, e.getMessage()), e);
        }
    }

    @Command(name = "list", description = "List installed agent skills.")
    public void list(
        @Option(names = {"--project-dir"}, description = "The project root directory. Defaults to the current working directory.") String projectDir,
        @Mixin OutputOptions outputOptions
    ) {
        Path projectRoot = IoUtils.getProjectPath(projectDir);

        try {
            SkillsLockfile lockfile = SkillsLockfile.load(projectRoot);

            if (lockfile.skills().isEmpty()) {
                outputOptions.info("No skills installed. Use 'arconia skills add' or 'arconia skills install' to get started.");
                return;
            }

            outputOptions.info("Installed skills (%d):".formatted(lockfile.skills().size()));
            outputOptions.info("");

            List<String> tableLines = SkillTableFormatter.formatInstalledSkills(lockfile);
            for (String line : tableLines) {
                outputOptions.info(line);
            }
        }
        catch (IOException e) {
            throw new CliException("Failed to read %s: %s".formatted(SkillsLockfile.FILENAME, e.getMessage()), e);
        }
    }

    @Command(name = "remove", description = "Remove agent skills from the project.")
    public void remove(
        @Option(names = {"--name"}, required = true, arity = "1..*", description = "The name of the agent skill to remove.") List<String> skillNames,
        @Option(names = {"-y", "--yes"}, description = "Skip confirmation prompt.") boolean skipConfirmation,
        @Option(names = {"--project-dir"}, description = "The project root directory. Defaults to the current working directory.") String projectDir,
        @Mixin OutputOptions outputOptions
    ) {
        Path projectRoot = IoUtils.getProjectPath(projectDir);

        try {
            SkillsManifest manifest = SkillsManifest.load(projectRoot);
            SkillsLockfile lockfile = SkillsLockfile.load(projectRoot);

            for (String skillName : skillNames) {
                // 1. Find the entry in the lock file to get the paths
                SkillsLockfile.LockfileEntry lockEntry = lockfile.findEntry(skillName);

                if (lockEntry != null) {
                    // 2. Confirm before deleting
                    if (!skipConfirmation) {
                        Path primaryDir = projectRoot.resolve(lockEntry.path());
                        if (Files.exists(primaryDir)) {
                            String pathsDescription = lockEntry.path();
                            if (lockEntry.additionalPaths() != null && !lockEntry.additionalPaths().isEmpty()) {
                                pathsDescription += " (+ %d additional)".formatted(lockEntry.additionalPaths().size());
                            }
                            boolean confirmed = confirm(
                                "Remove skill '%s' and delete %s? [y/N]".formatted(skillName, pathsDescription));
                            if (!confirmed) {
                                outputOptions.info("⏭ Skipped '%s'.".formatted(skillName));
                                continue;
                            }
                        }
                    }

                    // 3. Delete the primary skill directory (with safety validation)
                    Path primaryDir = projectRoot.resolve(lockEntry.path());
                    if (Files.exists(primaryDir)) {
                        IoUtils.deleteSkillDirectory(primaryDir, projectRoot, SkillInstaller.DEFAULT_SKILLS_PATH);
                        outputOptions.verbose("Deleted directory: %s".formatted(primaryDir));
                    }

                    // 4. Delete additional vendor symlinks (with path containment check)
                    if (lockEntry.additionalPaths() != null) {
                        for (String additionalPath : lockEntry.additionalPaths()) {
                            Path additionalDir = projectRoot.resolve(additionalPath).normalize();
                            if (!additionalDir.toAbsolutePath().startsWith(projectRoot.toAbsolutePath().normalize())) {
                                outputOptions.error("Skipping '%s': path resolves outside the project root.".formatted(additionalPath));
                                continue;
                            }
                            IoUtils.deleteSymlinkOrDirectory(additionalDir);
                            outputOptions.verbose("Deleted symlink: %s".formatted(additionalDir));
                        }
                    }

                    // 5. Remove from lock file
                    lockfile = lockfile.removeEntry(skillName);
                    outputOptions.info("Removed skill '%s'.".formatted(skillName));
                }
                else {
                    outputOptions.verbose("Skill '%s' not found in %s, skipping.".formatted(skillName, SkillsLockfile.FILENAME));
                }

                // 6. Remove from manifest (always, even if not in lockfile)
                if (manifest.findSkill(skillName) != null) {
                    manifest = manifest.removeSkill(skillName);
                }
            }

            // 7. Save updated files
            manifest.save(projectRoot);
            lockfile.save(projectRoot);
        }
        catch (IOException e) {
            throw new CliException("Failed to remove skill: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "update", description = "Update agent skills in the project.")
    public void update(
        @Option(names = {"--name"}, arity = "1..*", description = "The name of the agent skill to update.") List<String> skillNames,
        @Option(names = {"--all"}, description = "Update all agent skills in the project.") boolean updateAll,
        @Option(names = {"--project-dir"}, description = "The project root directory. Defaults to the current working directory.") String projectDir,
        @Mixin OutputOptions outputOptions
    ) {
        Path projectRoot = IoUtils.getProjectPath(projectDir);

        outputOptions.info("Checking for skill updates...");

        try {
            SkillsLockfile lockfile = SkillsLockfile.load(projectRoot);
            SkillsManifest manifest = SkillsManifest.load(projectRoot);

            if (lockfile.skills().isEmpty()) {
                outputOptions.info("No skills installed. Nothing to update.");
                return;
            }

            // Determine which skills to update
            List<SkillsLockfile.LockfileEntry> toUpdate;
            if (updateAll || skillNames == null || skillNames.isEmpty()) {
                toUpdate = lockfile.skills();
            }
            else {
                toUpdate = lockfile.skills().stream()
                    .filter(entry -> skillNames.contains(entry.name()))
                    .toList();

                if (toUpdate.isEmpty()) {
                    outputOptions.info("None of the specified skills are installed.");
                    return;
                }
            }

            Registry ociRegistry = ArtifactRegistry.create();
            SkillInstaller installer = new SkillInstaller(ociRegistry);
            SkillUpdater updater = new SkillUpdater(ociRegistry, installer);
            int updatedCount = 0;

            for (SkillsLockfile.LockfileEntry entry : toUpdate) {
                try {
                    outputOptions.verbose("  Available tags for %s: %s".formatted(entry.name(), updater.listTags(entry)));

                    UpdateCheckResult checkResult = updater.checkForUpdate(entry);

                    // Re-read additionalBasePaths from skills.json (source of truth)
                    SkillsManifest.SkillEntry manifestEntry = manifest.findSkill(entry.name());
                    List<String> additionalBasePaths = (manifestEntry != null && manifestEntry.additionalBasePaths() != null)
                            ? manifestEntry.additionalBasePaths()
                            : Collections.emptyList();

                    switch (checkResult) {
                        case UpdateCheckResult.UpToDate upToDate -> {
                            outputOptions.info("  ✓ %s (%s) is up to date.".formatted(upToDate.name(), upToDate.tag()));
                        }
                        case UpdateCheckResult.UpdateAvailable updateAvailable -> {
                            outputOptions.info("  ⬆ Updating %s (%s → %s)...".formatted(
                                updateAvailable.name(), updateAvailable.currentTag(), updateAvailable.newestTag()));

                            SkillInstaller.InstallResult result = updater.applyUpdate(
                                updateAvailable.newestRef(), projectRoot, additionalBasePaths);
                            updatedCount++;

                            outputOptions.info("  ✅ Updated %s to %s".formatted(updateAvailable.name(), updateAvailable.newestTag()));
                            outputOptions.verbose("     Digest: %s".formatted(result.ref().digest()));
                        }
                    }
                }
                catch (Exception e) {
                    outputOptions.error("Failed to update %s: %s".formatted(entry.name(), e.getMessage()));
                }
            }

            if (updatedCount == 0) {
                outputOptions.info("All skills are up to date.");
            }
            else {
                outputOptions.info("Updated %d skill(s).".formatted(updatedCount));
            }
        }
        catch (IOException e) {
            throw new CliException("Failed to update skills: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "push", description = "Push agent skill(s) as OCI artifact(s).")
    public void push(
        @Option(names = {"--ref"}, required = true, description = "The OCI artifact reference. For a single skill: the full ref (e.g. ghcr.io/org/skills/my-skill). With --all: the base ref prefix (e.g. ghcr.io/org/skills).") String skillRef,
        @Option(names = {"--path"}, required = true, description = "The path to the skill directory (must contain SKILL.md), or with --all: the parent directory containing skill subdirectories.") String skillPath,
        @Option(names = {"--tag"}, required = true, description = "The semver version tag (e.g. 1.0.0). Non-semver tags like 'latest' can be applied via --additional-tag.") String tag,
        @Option(names = {"--additional-tag"}, arity = "0..*", description = "Additional tags to apply to the same artifact (e.g. --additional-tag latest --additional-tag 1).") List<String> additionalTags,
        @Option(names = {"--annotation"}, arity = "0..*", description = "Extra annotations in key=value format (e.g. --annotation org.opencontainers.image.vendor=arconia).") List<String> annotations,
        @Option(names = {"--all"}, description = "Discover and push all skills found as direct subdirectories of --path.") boolean pushAll,
        @Option(names = {"--output-report"}, arity = "0..1", fallbackValue = ArtifactPublishReport.DEFAULT_SKILLS_FILENAME, description = "Write a publish report file. Defaults to '" + ArtifactPublishReport.DEFAULT_SKILLS_FILENAME + "' when specified without a path.") String outputReport,
        @Mixin OutputOptions outputOptions
    ) {
        try {
            Path basePath = Path.of(skillPath).toAbsolutePath();
            Map<String, String> extraAnnotations = ArtifactAnnotations.parseAnnotations(annotations);

            Registry ociRegistry = ArtifactRegistry.create();
            SkillPublisher publisher = new SkillPublisher(ociRegistry);
            SkillBatchPublisher batchPublisher = new SkillBatchPublisher(publisher);

            if (pushAll) {
                pushAllSkills(batchPublisher, basePath, skillRef, tag, additionalTags, extraAnnotations, outputReport, outputOptions);
            } else {
                pushSingleSkill(batchPublisher, basePath, skillRef, tag, additionalTags, extraAnnotations, outputReport, outputOptions);
            }
        }
        catch (IOException e) {
            throw new CliException("Failed to publish skills: %s".formatted(e.getMessage()), e);
        }
        catch (IllegalArgumentException e) {
            throw new CliException("Invalid input: %s".formatted(e.getMessage()), e);
        }
    }

    /**
     * Pushes a single skill directory as an OCI artifact.
     */
    private void pushSingleSkill(SkillBatchPublisher batchPublisher, Path skillDirectory, String skillRef, String tag,
                                  List<String> additionalTags, Map<String, String> extraAnnotations,
                                  @Nullable String outputReport, OutputOptions outputOptions) throws IOException {

        outputOptions.newLine();
        outputOptions.info("Publishing skill '%s'...".formatted(skillDirectory.getFileName()));
        outputOptions.newLine();

        SkillPublisher.PublishResult result = batchPublisher.publishSingle(skillDirectory, skillRef, tag, additionalTags, extraAnnotations);
        SkillRef resolvedRef = result.ref();

        outputOptions.info("Published skill '%s'".formatted(result.config().name()));
        outputOptions.info("OCI Artifact: %s".formatted(resolvedRef.fullTagReference()));

        // Report additional tags
        if (additionalTags != null) {
            for (String additionalTag : additionalTags) {
                outputOptions.info("Tagged: %s:%s".formatted(resolvedRef.repository(), additionalTag));
            }
        }

        outputOptions.info("Digest: %s".formatted(resolvedRef.digest()));

        // Write publish report if requested
        if (outputReport != null) {
            ArtifactPublishReport report = ArtifactPublishReport.empty(DateTimeUtils.nowIso())
                .withArtifact(new ArtifactPublishReport.ArtifactEntry(
                    result.config().name(),
                    resolvedRef.fullTagReference(),
                    tag,
                    resolvedRef.digest(),
                    result.config().description()
                ));

            Path reportPath = Path.of(outputReport).toAbsolutePath();
            report.save(reportPath);

            outputOptions.newLine();
            outputOptions.info("Report: %s".formatted(reportPath));
        }
    }

    /**
     * Discovers and pushes all skill directories found under the given parent path.
     */
    private void pushAllSkills(SkillBatchPublisher batchPublisher, Path parentPath, String baseRef, String tag,
                               List<String> additionalTags, Map<String, String> extraAnnotations,
                               @Nullable String outputReport, OutputOptions outputOptions) throws IOException {

        List<Path> skillDirs = IoUtils.discoverSubDirectoriesWithFile(parentPath, SkillFrontmatterParser.SKILL_FILENAME);

        outputOptions.newLine();
        if (skillDirs.isEmpty()) {
            outputOptions.info("No skill directories found under: %s".formatted(parentPath));
            return;
        }

        outputOptions.info("Discovered %d skill(s) under %s".formatted(skillDirs.size(), parentPath));
        outputOptions.newLine();

        SkillBatchPublisher.BatchPublishResult batchResult = batchPublisher.publishAll(
            parentPath, baseRef, tag, additionalTags, extraAnnotations);

        // Report results
        ArtifactPublishReport report = ArtifactPublishReport.empty(DateTimeUtils.nowIso());

        for (BatchEntryResult entryResult : batchResult.results()) {
            switch (entryResult) {
                case BatchEntryResult.Success success -> {
                    SkillPublisher.PublishResult publishResult = success.publishResult();
                    SkillRef targetRef = success.targetRef();

                    outputOptions.info("Published skill '%s'".formatted(publishResult.config().name()));
                    outputOptions.info("OCI Artifact: %s".formatted(targetRef.fullTagReference()));

                    if (additionalTags != null) {
                        for (String additionalTag : additionalTags) {
                            outputOptions.info("Tagged: %s:%s".formatted(targetRef.repository(), additionalTag));
                        }
                    }

                    outputOptions.info("Digest: %s".formatted(publishResult.ref().digest()));

                    report = report.withArtifact(new ArtifactPublishReport.ArtifactEntry(
                        publishResult.config().name(),
                        targetRef.fullTagReference(),
                        tag,
                        publishResult.ref().digest(),
                        publishResult.config().description()
                    ));
                }
                case BatchEntryResult.Failure failure -> {
                    outputOptions.error("Failed to publish '%s': %s".formatted(failure.directoryName(), failure.errorMessage()));
                }
            }
            outputOptions.newLine();
        }

        outputOptions.info("Published %d/%d skill(s)".formatted(batchResult.successCount(), batchResult.totalCount()));

        // Write publish report if requested
        if (outputReport != null) {
            Path reportPath = Path.of(outputReport).toAbsolutePath();
            report.save(reportPath);
            outputOptions.newLine();
            outputOptions.info("Report: %s".formatted(reportPath));
        }
    }

    // --- Interactive confirmation ---

    /**
     * Prompts the user for confirmation using the JLine terminal.
     * Reads a single line and returns {@code true} if the user answers
     * "y" or "yes" (case-insensitive). Returns {@code false} for any other
     * input or if input cannot be read.
     *
     * @param prompt the prompt message (e.g., "Remove skill 'X'? [y/N]")
     * @return {@code true} if the user confirms
     */
    private boolean confirm(String prompt) {
        terminal.writer().print(prompt + " ");
        terminal.writer().flush();
        try {
            var reader = new LineNumberReader(terminal.reader());
            String line = reader.readLine();
            return line != null
                && (line.strip().equalsIgnoreCase("y") || line.strip().equalsIgnoreCase("yes"));
        }
        catch (IOException e) {
            return false;
        }
    }

}
