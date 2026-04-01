package io.arconia.cli.commands.skills;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import land.oras.ContainerRef;
import land.oras.Registry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.oci.OciRegistryProvider;
import io.arconia.cli.skills.ArtifactPublishReport;
import io.arconia.cli.skills.SkillCatalogPublisher;
import io.arconia.cli.skills.SkillCatalogService;
import io.arconia.cli.skills.SkillCatalogService.CatalogFetchResult;
import io.arconia.cli.skills.SkillCatalogService.CatalogPaths;
import io.arconia.cli.skills.SkillCatalogService.CatalogUpdateResult;
import io.arconia.cli.skills.SkillCatalogService.CatalogView;
import io.arconia.cli.skills.SkillCatalogService.RegisteredCatalog;
import io.arconia.cli.skills.SkillTableFormatter;
import io.arconia.cli.utils.DateTimeUtils;
import io.arconia.cli.utils.OciUtils;
import io.arconia.cli.utils.SemverUtils;

/**
 * CLI commands for managing agent skills catalogs.
 */
@Component
@Command(
    name = "catalog",
    description = "Manage agent skills catalogs."
)
public class SkillsCatalogCommands implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "push", description = "Build and push a skills catalog as an OCI Index.")
    public void push(
        @Option(names = {"--ref"}, required = true, description = "The OCI artifact reference for the catalog (e.g. ghcr.io/org/skills-catalog).") String catalogRef,
        @Option(names = {"--name"}, required = true, description = "The catalog identifier (e.g. arconia-io).") String catalogName,
        @Option(names = {"--tag"}, required = true, description = "The semver version tag for the catalog (e.g. 1.0.0). Non-semver tags like 'latest' can be applied via --additional-tag.") String tag,
        @Option(names = {"--additional-tag"}, arity = "0..*", description = "Additional tags to apply to the catalog (e.g. --additional-tag latest).") List<String> additionalTags,
        @Option(names = {"--from-report"}, description = "Path to a publish report file (from 'skills push --output-report').") String fromReport,
        @Option(names = {"--skill"}, arity = "0..*", description = "Explicit skill OCI references to include (e.g. --skill ghcr.io/org/skills/pull-request:1.0.0).") List<String> skillRefs,
        @Option(names = {"--description"}, description = "A short description of the catalog. Defaults to 'Agent Skills Catalog'.") String catalogDescription,
        @Option(names = {"--annotation"}, arity = "0..*", description = "Extra annotations in key=value format (e.g. --annotation org.opencontainers.image.vendor=arconia-io).") List<String> annotations,
        @Option(names = {"--output-report"}, arity = "0..1", fallbackValue = ArtifactPublishReport.DEFAULT_CATALOG_FILENAME, description = "Write a publish report file recording the published catalog artifact. Defaults to '" + ArtifactPublishReport.DEFAULT_CATALOG_FILENAME + "' when specified without a path.") String outputReport,
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
        outputOptions.info("Publishing skills catalog...");
        outputOptions.newLine();

        try {
            Map<String, String> extraAnnotations = OciUtils.parseAnnotations(annotations);
            if (catalogDescription != null && !catalogDescription.isBlank()) {
                extraAnnotations.put("org.opencontainers.image.description", catalogDescription);
            }

            Registry ociRegistry = OciRegistryProvider.create();
            SkillCatalogPublisher publisher = new SkillCatalogPublisher(ociRegistry);

            String fullRef = "%s:%s".formatted(catalogRef, tag);
            ContainerRef containerRef = ContainerRef.parse(fullRef);

            SkillCatalogPublisher.PublishResult result;

            if (hasReport) {
                Path reportPath = Path.of(fromReport).toAbsolutePath();
                outputOptions.verbose("Loading publish report from: %s".formatted(reportPath));

                ArtifactPublishReport report = ArtifactPublishReport.load(reportPath);
                outputOptions.info("Loaded publish report with %d artifact(s).".formatted(report.artifacts().size()));

                result = publisher.publishFromReport(report, containerRef, catalogName, tag, extraAnnotations);
            }
            else {
                outputOptions.info("Resolving %d skill reference(s) from registry...".formatted(skillRefs.size()));
                result = publisher.publishFromRefs(skillRefs, containerRef, catalogName, tag, extraAnnotations);
            }

            outputOptions.newLine();
            outputOptions.info("Published catalog '%s'".formatted(catalogName));
            outputOptions.info("OCI Artifact: %s".formatted(fullRef));
            outputOptions.info("Skills: %d".formatted(result.skillCount()));

            if (additionalTags != null) {
                for (String additionalTag : additionalTags) {
                    publisher.tag(result, catalogRef, additionalTag);
                    outputOptions.info("Tagged: %s:%s".formatted(catalogRef, additionalTag));
                }
            }

            outputOptions.info("Digest: %s".formatted(result.digest()));

            // Write publish report if requested
            if (outputReport != null) {
                ArtifactPublishReport publishReport = ArtifactPublishReport.empty(DateTimeUtils.nowIso())
                    .withArtifact(new ArtifactPublishReport.ArtifactEntry(
                        catalogName,
                        fullRef,
                        tag,
                        result.digest(),
                        catalogDescription
                    ));

                Path reportPath = Path.of(outputReport).toAbsolutePath();
                publishReport.save(reportPath);

                outputOptions.newLine();
                outputOptions.info("Report: %s".formatted(reportPath));
            }
        }
        catch (IOException e) {
            throw new CliException("Failed to publish catalog: %s".formatted(e.getMessage()), e);
        }
        catch (IllegalArgumentException e) {
            throw new CliException("Invalid input: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "add", description = "Register a catalog and fetch its contents into the local cache.")
    public void add(
        @Option(names = {"--name"}, required = true, description = "A short alias for the catalog (e.g. arconia).") String catalogName,
        @Option(names = {"--ref"}, required = true, description = "The OCI artifact reference for the catalog (e.g. ghcr.io/org/skills-catalog:latest).") String catalogRef,
        @Mixin OutputOptions outputOptions
    ) {
        outputOptions.newLine();
        outputOptions.info("Adding catalog '%s'...".formatted(catalogName));
        outputOptions.newLine();

        try {
            Registry ociRegistry = OciRegistryProvider.create();
            SkillCatalogService catalogService = new SkillCatalogService(ociRegistry);

            boolean existed = catalogService.addCatalog(catalogName, catalogRef);

            String action = existed ? "Updated" : "Registered";
            outputOptions.info("%s catalog '%s' → %s".formatted(action, catalogName, catalogRef));

            CatalogPaths paths = catalogService.getCatalogPaths(catalogName);
            outputOptions.verbose("Config: %s".formatted(paths.configPath()));
            outputOptions.verbose("Cache:  %s".formatted(paths.cachePath()));
        }
        catch (IOException e) {
            throw new CliException("Failed to add catalog: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "remove", description = "Remove a catalog from the local Arconia CLI configuration.")
    public void remove(
        @Option(names = {"--name"}, required = true, description = "The alias of the catalog to remove.") String catalogName,
        @Mixin OutputOptions outputOptions
    ) {
        try {
            SkillCatalogService catalogService = new SkillCatalogService(OciRegistryProvider.create());
            RegisteredCatalog removed = catalogService.removeCatalog(catalogName);

            if (removed == null) {
                outputOptions.info("No catalog named '%s' is registered.".formatted(catalogName));
                return;
            }

            outputOptions.info("Removed catalog '%s' (%s).".formatted(removed.name(), removed.ref()));
        }
        catch (IOException e) {
            throw new CliException("Failed to remove catalog: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "update", description = "Check for newer versions of registered catalogs and update them.")
    public void update(
        @Option(names = {"--name"}, description = "The alias of a specific catalog to update. If omitted, updates all registered catalogs.") String catalogName,
        @Mixin OutputOptions outputOptions
    ) {
        try {
            Registry ociRegistry = OciRegistryProvider.create();
            SkillCatalogService catalogService = new SkillCatalogService(ociRegistry);

            // Determine which catalog names to update
            List<String> catalogNames;
            if (catalogName != null && !catalogName.isBlank()) {
                catalogNames = List.of(catalogName);
            }
            else {
                List<RegisteredCatalog> all = catalogService.listRegisteredCatalogs();
                if (all.isEmpty()) {
                    outputOptions.info("No catalogs registered. Use 'catalog add' to register one.");
                    return;
                }
                catalogNames = all.stream().map(RegisteredCatalog::name).toList();
            }

            int updatedCount = 0;

            for (String name : catalogNames) {
                try {
                    outputOptions.verbose("Checking catalog '%s'...".formatted(name));

                    CatalogUpdateResult checkResult = catalogService.checkForUpdate(name);

                    switch (checkResult) {
                        case CatalogUpdateResult.UpToDate upToDate -> {
                            outputOptions.info("  ✓ %s (%s) is up to date.".formatted(upToDate.name(), upToDate.tag()));
                        }
                        case CatalogUpdateResult.NewVersionAvailable newVersion -> {
                            outputOptions.info("⬆ Updating catalog '%s' (%s → %s)...".formatted(
                                newVersion.name(), newVersion.currentTag(), newVersion.newestTag()));
                            catalogService.applyUpdate(newVersion.name(), newVersion.newestRef());
                            outputOptions.info("✅ Updated catalog '%s' → %s".formatted(newVersion.name(), newVersion.newestRef()));
                            updatedCount++;
                        }
                        case CatalogUpdateResult.DigestChanged digestChanged -> {
                            outputOptions.info("🔄 Refreshing catalog '%s' (%s)...".formatted(
                                digestChanged.name(), digestChanged.tag()));
                            catalogService.applyUpdate(digestChanged.name(), digestChanged.ref());
                            outputOptions.info("✅ Refreshed catalog '%s' (%s).".formatted(digestChanged.name(), digestChanged.tag()));
                            updatedCount++;
                        }
                    }
                }
                catch (Exception e) {
                    outputOptions.error("Failed to update catalog '%s': %s".formatted(name, e.getMessage()));
                }
            }

            if (updatedCount == 0) {
                outputOptions.info("All catalogs are up to date.");
            }
            else {
                outputOptions.info("Updated %d catalog(s).".formatted(updatedCount));
            }
        }
        catch (CliException e) {
            throw e;
        }
        catch (IOException e) {
            throw new CliException("Failed to update catalogs: %s".formatted(e.getMessage()), e);
        }
    }

    @Command(name = "list", description = "List available skills in a catalog. Uses --ref for a direct reference, or --name for a registered catalog, or lists all registered catalogs if neither is provided.")
    public void list(
        @Option(names = {"--ref"}, required = false, description = "The OCI artifact reference for the catalog.") String catalogRef,
        @Option(names = {"--name"}, required = false, description = "The alias of a registered catalog to list.") String catalogName,
        @Mixin OutputOptions outputOptions
    ) {
        outputOptions.newLine();

        try {
            Registry ociRegistry = OciRegistryProvider.create();
            SkillCatalogService catalogService = new SkillCatalogService(ociRegistry);

            if (catalogRef != null && !catalogRef.isBlank()) {
                // Direct ref: always fetch live (no cache)
                outputOptions.verbose("Fetching catalog: %s".formatted(catalogRef));

                CatalogView view = catalogService.fetchCatalogFromRef(catalogRef);
                displayCatalogView(view, outputOptions);
            }
            else if (catalogName != null && !catalogName.isBlank()) {
                // Registered catalog by name — uses cache with staleness-aware refresh
                listRegisteredCatalog(catalogService, catalogName, outputOptions);
            }
            else {
                // No ref or name: list all registered catalogs
                List<RegisteredCatalog> catalogs = catalogService.listRegisteredCatalogs();

                if (catalogs.isEmpty()) {
                    outputOptions.info("No catalogs registered. Use 'catalog add --name <alias> --ref <ref>' to register one,");
                    outputOptions.info("or 'catalog list --ref <ref>' to list skills from a specific catalog.");
                    return;
                }

                for (RegisteredCatalog catalog : catalogs) {
                    try {
                        listRegisteredCatalog(catalogService, catalog.name(), outputOptions);
                    }
                    catch (Exception e) {
                        outputOptions.info("Failed to list catalog '%s' (%s): %s".formatted(
                            catalog.name(), catalog.ref(), e.getMessage()));
                    }
                    outputOptions.info("");
                }
            }
        }
        catch (CliException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CliException("Failed to list catalogs: %s".formatted(e.getMessage()), e);
        }
    }

    /**
     * Lists a registered catalog, delegating cache management entirely to the service.
     */
    private void listRegisteredCatalog(SkillCatalogService catalogService, String catalogName,
                                        OutputOptions outputOptions) throws IOException {
        CatalogFetchResult fetchResult = catalogService.getRegisteredCatalog(catalogName);

        switch (fetchResult) {
            case CatalogFetchResult.Success success -> {
                outputOptions.verbose("Using catalog '%s' (source: %s).".formatted(catalogName, success.source()));
                displayCatalogView(success.view(), outputOptions);
            }
            case CatalogFetchResult.Failure failure -> {
                throw new CliException(failure.errorMessage());
            }
        }
    }

    /**
     * Displays a catalog view (header + skill table + verbose details).
     */
    private void displayCatalogView(CatalogView view, OutputOptions outputOptions) {
        // Header
        String header = view.catalogName();
        if (view.catalogVersion() != null) {
            header += " (v%s)".formatted(view.catalogVersion());
        }
        outputOptions.info("📦 %s".formatted(header));

        if (view.description() != null) {
            outputOptions.info("   %s".formatted(view.description()));
        }
        outputOptions.info("");

        if (view.skills().isEmpty()) {
            outputOptions.info("   No skills found in this catalog.");
            return;
        }

        outputOptions.info("Available skills (%d):".formatted(view.skills().size()));
        outputOptions.info("");

        // Table
        List<String> tableLines = SkillTableFormatter.formatCatalogSkills(view.skills());
        for (String line : tableLines) {
            outputOptions.info(line);
        }
    }

}
