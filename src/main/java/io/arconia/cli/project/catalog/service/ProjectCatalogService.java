package io.arconia.cli.project.catalog.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import land.oras.ContainerRef;
import land.oras.Index;
import land.oras.Registry;

import io.arconia.cli.artifact.ArtifactAnnotations;
import io.arconia.cli.artifact.VersionSelector;
import io.arconia.cli.commands.options.CliTableFormatter;
import io.arconia.cli.commands.options.OutputOptions;

/**
 * Service for managing project catalogs.
 */
public final class ProjectCatalogService {

    public static final String BUILT_IN_CATALOG_NAME = "arconia-templates";

    public static final String BUILT_IN_CATALOG_REF = "ghcr.io/arconia-io/arconia-templates/catalog";

    private static final List<String> PROJECT_TABLE_HEADERS = List.of("NAME", "DESCRIPTION", "TYPE", "LABELS");

    private final Registry registry;
    private final OutputOptions outputOptions;

    public ProjectCatalogService(Registry registry, OutputOptions outputOptions) throws IOException {
        Assert.notNull(registry, "registry cannot be null");
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        this.registry = registry;
        this.outputOptions = outputOptions;
    }

    /**
     * Ensures the built-in Arconia project catalog is registered.
     * <p>
     * Auto-registers the built-in catalog if:
     * <ol>
     *   <li>The built-in catalog is not found in the registry</li>
     *   <li>The "builtInCatalogDismissed" flag is {@code false} or absent</li>
     * </ol>
     * Network failures are logged but do not block the caller.
     */
    public void ensureBuiltInCatalogRegistered() {
        try {
            ProjectCatalogRegistry.CatalogEntry existing =
                    ProjectCatalogRegistry.findCatalog(BUILT_IN_CATALOG_NAME);
            if (existing != null) {
                return;
            }

            ProjectCatalogRegistry registry = ProjectCatalogRegistry.load();
            if (registry.builtInCatalogDismissed()) {
                return;
            }

            outputOptions.info("Registering built-in catalog '%s'...".formatted(BUILT_IN_CATALOG_NAME));
            addCatalog(BUILT_IN_CATALOG_NAME, BUILT_IN_CATALOG_REF);
        }
        catch (Exception e) {
            outputOptions.verbose("Could not register built-in catalog: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Adds a catalog to the registry and cache.
     */
    public void addCatalog(String catalogName, String catalogRef) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");
        Assert.hasText(catalogRef, "catalogRef cannot be null or empty");

        outputOptions.info("Adding catalog '%s'...".formatted(catalogName));
        Index index = registry.getIndex(ContainerRef.parse(catalogRef));
        ProjectCatalogCache.save(catalogName, index);
        outputOptions.verbose("Catalog '%s' saved to cache: %s".formatted(catalogName, ProjectCatalogCache.resolveCacheFileForCatalog(catalogName)));
        ProjectCatalogRegistry.addCatalog(ProjectCatalogRegistry.CatalogEntry.builder()
                        .name(catalogName)
                        .ref(catalogRef)
                        .digest(index.getDescriptor().getDigest())
                        .lastFetchedAt(Instant.now())
                        .build());

        if (BUILT_IN_CATALOG_NAME.equals(catalogName)) {
            ProjectCatalogRegistry currentRegistry = ProjectCatalogRegistry.load();
            if (currentRegistry.builtInCatalogDismissed()) {
                ProjectCatalogRegistry.save(currentRegistry.mutate()
                        .builtInCatalogDismissed(false)
                        .build());
            }
        }

        outputOptions.verbose("Catalog '%s' added to registry: %s.".formatted(catalogName, ProjectCatalogRegistry.registryPath()));
        outputOptions.info("Catalog '%s' added.".formatted(catalogName));
        outputOptions.info("OCI Artifact: %s".formatted(catalogRef));
        outputOptions.info("Digest: %s".formatted(index.getDescriptor().getDigest()));
    }

    /**
     * Updates a catalog in the registry and cache by re-fetching its OCI index.
     * Checks for a newer semver tag; if found, updates the stored ref to the latest version.
     */
    public void updateCatalog(String catalogName) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");

        ProjectCatalogRegistry.CatalogEntry existing = ProjectCatalogRegistry.findCatalog(catalogName);
        if (existing == null) {
            outputOptions.error("Catalog '%s' is not registered. Run 'arconia template catalog add' to register it first.".formatted(catalogName));
            return;
        }

        ContainerRef existingRef = ContainerRef.parse(existing.ref());
        List<String> allTags = registry.getTags(existingRef).tags();
        String highestTag = VersionSelector.highestVersion(allTags);

        String newRef;
        if (StringUtils.hasText(highestTag)) {
            newRef = "%s/%s:%s".formatted(existingRef.getRegistry(), existingRef.getFullRepository(), highestTag);
        } else {
            newRef = existing.ref();
        }

        outputOptions.info("Updating catalog '%s'...".formatted(catalogName));
        Index index = registry.getIndex(ContainerRef.parse(newRef));
        ProjectCatalogCache.save(catalogName, index);
        outputOptions.verbose("Catalog '%s' saved to cache: %s".formatted(catalogName, ProjectCatalogCache.resolveCacheFileForCatalog(catalogName)));
        ProjectCatalogRegistry.addCatalog(ProjectCatalogRegistry.CatalogEntry.builder()
                .name(catalogName)
                .ref(newRef)
                .digest(index.getDescriptor().getDigest())
                .lastFetchedAt(Instant.now())
                .build());
        outputOptions.verbose("Catalog '%s' updated in registry: %s.".formatted(catalogName, ProjectCatalogRegistry.registryPath()));
        outputOptions.info("Catalog '%s' updated.".formatted(catalogName));
        outputOptions.info("OCI Artifact: %s".formatted(newRef));
        outputOptions.info("Digest: %s".formatted(index.getDescriptor().getDigest()));
    }

    /**
     * Removes a catalog from the registry and cache.
     */
    public void removeCatalog(String catalogName) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");
        outputOptions.info("Removing catalog '%s'...".formatted(catalogName));
        ProjectCatalogCache.delete(catalogName);
        ProjectCatalogRegistry.removeCatalog(catalogName);

        if (BUILT_IN_CATALOG_NAME.equals(catalogName)) {
            ProjectCatalogRegistry currentRegistry = ProjectCatalogRegistry.load();
            ProjectCatalogRegistry.save(currentRegistry.mutate()
                    .builtInCatalogDismissed(true)
                    .build());
        }

        outputOptions.info("Catalog '%s' removed.".formatted(catalogName));
    }

    /**
     * Lists all projects in a catalog.
     */
    public void listCatalog(String catalogName) throws IOException {
        Assert.hasText(catalogName, "catalogName cannot be null or empty");

        var catalog = ProjectCatalogRegistry.findCatalog(catalogName);
        outputOptions.verbose("Catalog '%s' found in registry: %s".formatted(catalogName, catalog));
        if (catalog == null) {
            outputOptions.error("Catalog '%s' is not registered. Run 'arconia template catalog add' to register it first.".formatted(catalogName));
            return;
        }

        var index = ProjectCatalogCache.load(catalogName);
        outputOptions.verbose("Catalog '%s' cache loaded: %s".formatted(catalogName, index != null));
        if (index == null) {
            outputOptions.error("No cached data found for catalog '%s'. Run 'arconia template catalog add' to register it first.".formatted(catalogName));
            return;
        }

        var catalogCard = ProjectCatalogCard.builder()
                .header(ProjectCatalogHeader.builder()
                        .name(catalog.name())
                        .ref(catalog.ref())
                        .description(index.getAnnotations().get(ArtifactAnnotations.OCI_DESCRIPTION))
                        .build())
                .summaries(ProjectCatalogCache.toProjectSummaries(index))
                .build();

        printCatalogHeader(outputOptions, catalogCard.header());
        outputOptions.newLine();
        outputOptions.table(CliTableFormatter.format(outputOptions.colorScheme(), PROJECT_TABLE_HEADERS, toProjectRows(catalogCard.summaries()), 3));
    }

    private void printCatalogHeader(OutputOptions outputOptions, ProjectCatalogHeader header) {
        outputOptions.info("📦 %s (%s)".formatted(header.name(), header.ref()));
        if (StringUtils.hasText(header.description())) {
            outputOptions.info("   %s".formatted(header.description()));
        }
    }

    private List<List<String>> toProjectRows(List<ProjectCatalogSummary> summaries) {
        return summaries.stream()
                .map(s -> List.of(s.name(), s.description(), s.type(), String.join(", ", s.labels())))
                .toList();
    }

}
