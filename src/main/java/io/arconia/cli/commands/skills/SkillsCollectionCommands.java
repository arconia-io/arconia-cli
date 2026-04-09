package io.arconia.cli.commands.skills;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.arconia.cli.commands.options.RegistryOptions;

import org.springframework.stereotype.Component;

import land.oras.ContainerRef;
import land.oras.Registry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.artifact.ArtifactRegistry;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.skills.ArtifactPublishReport;
import io.arconia.cli.skills.SkillCollectionPublisher;
import io.arconia.cli.skills.SkillCollectionService;
import io.arconia.cli.skills.SkillCollectionService.CollectionFetchResult;
import io.arconia.cli.skills.SkillCollectionService.CollectionPaths;
import io.arconia.cli.skills.SkillCollectionService.CollectionUpdateResult;
import io.arconia.cli.skills.SkillCollectionService.CollectionView;
import io.arconia.cli.skills.SkillCollectionService.RegisteredCollection;
import io.arconia.cli.skills.SkillTableFormatter;
import io.arconia.cli.utils.DateTimeUtils;
import io.arconia.cli.utils.SemverUtils;

@Component
@Command(
    name = "collection",
    description = "Manage agent skills collections."
)
public class SkillsCollectionCommands implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "push", description = "Build and push a skills collection as an OCI Index.")
    public void push(
        @Option(names = {"--ref"}, required = true, description = "The OCI artifact reference for the collection (e.g. ghcr.io/org/skills-collection).") String collectionRef,
        @Option(names = {"--name"}, required = true, description = "The collection identifier (e.g. arconia-io).") String collectionName,
        @Option(names = {"--tag"}, required = true, description = "The semver version tag for the collection (e.g. 1.0.0). Non-semver tags like 'latest' can be applied via --additional-tag.") String tag,
        @Option(names = {"--additional-tag"}, arity = "0..*", description = "Additional tags to apply to the collection (e.g. --additional-tag latest).") List<String> additionalTags,
        @Option(names = {"--from-report"}, description = "Path to a publish report file (from 'skills push --output-report').") String fromReport,
        @Option(names = {"--skill"}, arity = "0..*", description = "Explicit skill OCI references to include (e.g. --skill ghcr.io/org/skills/pull-request:1.0.0).") List<String> skillRefs,
        @Option(names = {"--description"}, description = "A short description of the collection. Defaults to 'Agent Skills Collection'.") String collectionDescription,
        @Option(names = {"--annotation"}, arity = "0..*", description = "Extra annotations in key=value format (e.g. --annotation org.opencontainers.image.vendor=arconia-io).") List<String> annotations,
        @Option(names = {"--output-report"}, arity = "0..1", fallbackValue = ArtifactPublishReport.DEFAULT_COLLECTION_FILENAME, description = "Write a publish report file recording the published collection artifact. Defaults to '" + ArtifactPublishReport.DEFAULT_COLLECTION_FILENAME + "' when specified without a path.") String outputReport,
        @Mixin RegistryOptions registryOptions,
        @Mixin OutputOptions outputOptions
    ) {
        boolean hasReport = fromReport != null && !fromReport.isBlank();
        boolean hasSkillRefs = skillRefs != null && !skillRefs.isEmpty();

        if (!hasReport && !hasSkillRefs) {
            throw new CliException("At least one of --from-report or --skill must be provided.");
        }

        if (!SemverUtils.isSemver(tag)) {
            throw new CliException("Tag '%s' is not a valid semver version (e.g. 1.0.0). Use --additional-tag for non-semver tags like 'latest'.".formatted(tag));
        }

        outputOptions.newLine();
        outputOptions.info("Publishing skills collection...");
        outputOptions.newLine();

        try {
            Map<String, String> extraAnnotations = ArtifactAnnotations.parseAnnotations(annotations);
            if (collectionDescription != null && !collectionDescription.isBlank()) {
                extraAnnotations.put("org.opencontainers.image.description", collectionDescription);
            }

            Registry ociRegistry = ArtifactRegistry.create(registryOptions);
            SkillCollectionPublisher publisher = new SkillCollectionPublisher(ociRegistry);

            String fullRef = "%s:%s".formatted(collectionRef, tag);
            ContainerRef containerRef = ContainerRef.parse(fullRef);

            SkillCollectionPublisher.PublishResult result;

            if (hasReport) {
                Path reportPath = Path.of(fromReport).toAbsolutePath();
                outputOptions.verbose("Loading publish report from: %s".formatted(reportPath));

                ArtifactPublishReport report = ArtifactPublishReport.load(reportPath);
                outputOptions.info("Loaded publish report with %d artifact(s).".formatted(report.artifacts().size()));

                result = publisher.publishFromReport(report, containerRef, collectionName, tag, extraAnnotations);
            }
            else {
                outputOptions.info("Resolving %d skill reference(s) from registry...".formatted(skillRefs.size()));
                result = publisher.publishFromRefs(skillRefs, containerRef, collectionName, tag, extraAnnotations);
            }

            outputOptions.newLine();
            outputOptions.info("Published collection '%s'".formatted(collectionName));
            outputOptions.info("OCI Artifact: %s".formatted(fullRef));
            outputOptions.info("Skills: %d".formatted(result.skillCount()));

            if (additionalTags != null) {
                for (String additionalTag : additionalTags) {
                    publisher.tag(result, collectionRef, additionalTag);
                    outputOptions.info("Tagged: %s:%s".formatted(collectionRef, additionalTag));
                }
            }

            outputOptions.info("Digest: %s".formatted(result.digest()));

            // Write publish report if requested
            if (outputReport != null) {
                ArtifactPublishReport publishReport = ArtifactPublishReport.empty(DateTimeUtils.nowIso())
                    .withArtifact(new ArtifactPublishReport.ArtifactEntry(
                        collectionName,
                        fullRef,
                        tag,
                        result.digest(),
                        collectionDescription
                    ));

                Path reportPath = Path.of(outputReport).toAbsolutePath();
                publishReport.save(reportPath);

                outputOptions.newLine();
                outputOptions.info("Report: %s".formatted(reportPath));
            }
        }
        catch (IOException e) {
            throw new CliException("Failed to publish collection: %s".formatted(e.getMessage()), e);
        }
        catch (IllegalArgumentException e) {
            throw new CliException("Invalid input: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "add", description = "Register a collection and fetch its contents into the local cache.")
    public void add(
        @Option(names = {"--name"}, required = true, description = "A short alias for the collection (e.g. arconia).") String collectionName,
        @Option(names = {"--ref"}, required = true, description = "The OCI artifact reference for the collection (e.g. ghcr.io/org/skills-collection:latest).") String collectionRef,
        @Mixin RegistryOptions registryOptions,
        @Mixin OutputOptions outputOptions
    ) {
        outputOptions.newLine();
        outputOptions.info("Adding collection '%s'...".formatted(collectionName));
        outputOptions.newLine();

        try {
            Registry ociRegistry = ArtifactRegistry.create(registryOptions);
            SkillCollectionService collectionService = new SkillCollectionService(ociRegistry);

            boolean existed = collectionService.addCollection(collectionName, collectionRef);

            String action = existed ? "Updated" : "Registered";
            outputOptions.info("%s collection '%s' → %s".formatted(action, collectionName, collectionRef));

            CollectionPaths paths = collectionService.getCollectionPaths(collectionName);
            outputOptions.verbose("Config: %s".formatted(paths.configPath()));
            outputOptions.verbose("Cache:  %s".formatted(paths.cachePath()));
        }
        catch (IOException e) {
            throw new CliException("Failed to add collection: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "remove", description = "Remove a collection from the local Arconia CLI configuration.")
    public void remove(
        @Option(names = {"--name"}, required = true, description = "The alias of the collection to remove.") String collectionName,
        @Mixin RegistryOptions registryOptions,
        @Mixin OutputOptions outputOptions
    ) {
        try {
            SkillCollectionService collectionService = new SkillCollectionService(ArtifactRegistry.create(registryOptions));
            RegisteredCollection removed = collectionService.removeCollection(collectionName);

            if (removed == null) {
                outputOptions.info("No collection named '%s' is registered.".formatted(collectionName));
                return;
            }

            outputOptions.info("Removed collection '%s' (%s).".formatted(removed.name(), removed.ref()));
        }
        catch (IOException e) {
            throw new CliException("Failed to remove collection: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "update", description = "Check for newer versions of registered collections and update them.")
    public void update(
        @Option(names = {"--name"}, description = "The alias of a specific collection to update. If omitted, updates all registered collections.") String collectionName,
        @Mixin RegistryOptions registryOptions,
        @Mixin OutputOptions outputOptions
    ) {
        try {
            Registry ociRegistry = ArtifactRegistry.create(registryOptions);
            SkillCollectionService collectionService = new SkillCollectionService(ociRegistry);

            // Determine which collection names to update
            List<String> collectionNames;
            if (collectionName != null && !collectionName.isBlank()) {
                collectionNames = List.of(collectionName);
            }
            else {
                List<RegisteredCollection> all = collectionService.listRegisteredCollections();
                if (all.isEmpty()) {
                    outputOptions.info("No collections registered. Use 'collection add' to register one.");
                    return;
                }
                collectionNames = all.stream().map(RegisteredCollection::name).toList();
            }

            int updatedCount = 0;

            for (String name : collectionNames) {
                try {
                    outputOptions.verbose("Checking collection '%s'...".formatted(name));

                    CollectionUpdateResult checkResult = collectionService.checkForUpdate(name);

                    switch (checkResult) {
                        case CollectionUpdateResult.UpToDate upToDate -> {
                            outputOptions.info("  ✓ %s (%s) is up to date.".formatted(upToDate.name(), upToDate.tag()));
                        }
                        case CollectionUpdateResult.NewVersionAvailable newVersion -> {
                            outputOptions.info("⬆ Updating collection '%s' (%s → %s)...".formatted(
                                newVersion.name(), newVersion.currentTag(), newVersion.newestTag()));
                            collectionService.applyUpdate(newVersion.name(), newVersion.newestRef());
                            outputOptions.info("✅ Updated collection '%s' → %s".formatted(newVersion.name(), newVersion.newestRef()));
                            updatedCount++;
                        }
                        case CollectionUpdateResult.DigestChanged digestChanged -> {
                            outputOptions.info("🔄 Refreshing collection '%s' (%s)...".formatted(
                                digestChanged.name(), digestChanged.tag()));
                            collectionService.applyUpdate(digestChanged.name(), digestChanged.ref());
                            outputOptions.info("✅ Refreshed collection '%s' (%s).".formatted(digestChanged.name(), digestChanged.tag()));
                            updatedCount++;
                        }
                    }
                }
                catch (Exception e) {
                    outputOptions.error("Failed to update collection '%s': %s".formatted(name, e.getMessage()));
                }
            }

            if (updatedCount == 0) {
                outputOptions.info("All collections are up to date.");
            }
            else {
                outputOptions.info("Updated %d collection(s).".formatted(updatedCount));
            }
        }
        catch (IOException e) {
            throw new CliException("Failed to update collections: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "list", description = "List available skills in a collection. Uses --ref for a direct reference, or --name for a registered collection, or lists all registered collections if neither is provided.")
    public void list(
        @Option(names = {"--ref"}, required = false, description = "The OCI artifact reference for the collection.") String collectionRef,
        @Option(names = {"--name"}, required = false, description = "The alias of a registered collection to list.") String collectionName,
        @Mixin RegistryOptions registryOptions,
        @Mixin OutputOptions outputOptions
    ) {
        outputOptions.newLine();

        try {
            Registry ociRegistry = ArtifactRegistry.create(registryOptions);
            SkillCollectionService collectionService = new SkillCollectionService(ociRegistry);

            if (collectionRef != null && !collectionRef.isBlank()) {
                // Direct ref: always fetch live (no cache)
                outputOptions.verbose("Fetching collection: %s".formatted(collectionRef));

                CollectionView view = collectionService.fetchCollectionFromRef(collectionRef);
                displayCollectionView(view, outputOptions);
            }
            else if (collectionName != null && !collectionName.isBlank()) {
                // Registered collection by name — uses cache with staleness-aware refresh
                listRegisteredCollection(collectionService, collectionName, outputOptions);
            }
            else {
                // No ref or name: list all registered collections
                List<RegisteredCollection> collections = collectionService.listRegisteredCollections();

                if (collections.isEmpty()) {
                    outputOptions.info("No collections registered. Use 'collection add --name <alias> --ref <ref>' to register one,");
                    outputOptions.info("or 'collection list --ref <ref>' to list skills from a specific collection.");
                    return;
                }

                for (RegisteredCollection collection : collections) {
                    try {
                        listRegisteredCollection(collectionService, collection.name(), outputOptions);
                    }
                    catch (Exception e) {
                        outputOptions.info("Failed to list collection '%s' (%s): %s".formatted(
                            collection.name(), collection.ref(), e.getMessage()));
                    }
                    outputOptions.info("");
                }
            }
        }
        catch (CliException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CliException("Failed to list collections: %s".formatted(e.getMessage()), e);
        }
    }

    /**
     * Lists a registered collection, delegating cache management entirely to the service.
     */
    private void listRegisteredCollection(SkillCollectionService collectionService, String collectionName,
                                        OutputOptions outputOptions) throws IOException {
        CollectionFetchResult fetchResult = collectionService.getRegisteredCollection(collectionName);

        switch (fetchResult) {
            case CollectionFetchResult.Success success -> {
                outputOptions.verbose("Using collection '%s' (source: %s).".formatted(collectionName, success.source()));
                displayCollectionView(success.view(), outputOptions);
            }
            case CollectionFetchResult.Failure failure -> {
                throw new CliException(failure.errorMessage());
            }
        }
    }

    /**
     * Displays a collection view (header + skill table + verbose details).
     */
    private void displayCollectionView(CollectionView view, OutputOptions outputOptions) {
        // Header
        String header = view.collectionName();
        if (view.collectionVersion() != null) {
            header += " (v%s)".formatted(view.collectionVersion());
        }
        outputOptions.info("📦 %s".formatted(header));

        if (view.description() != null) {
            outputOptions.info("   %s".formatted(view.description()));
        }
        outputOptions.info("");

        if (view.skills().isEmpty()) {
            outputOptions.info("   No skills found in this collection.");
            return;
        }

        outputOptions.info("Available skills (%d):".formatted(view.skills().size()));
        outputOptions.info("");

        // Table
        List<String> tableLines = SkillTableFormatter.formatCollectionSkills(view.skills());
        for (String line : tableLines) {
            outputOptions.info(line);
        }
    }

}
