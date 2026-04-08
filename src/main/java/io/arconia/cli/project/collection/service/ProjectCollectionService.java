package io.arconia.cli.project.collection.service;

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
 * Service for managing project collections.
 */
public final class ProjectCollectionService {

    private static final List<String> PROJECT_TABLE_HEADERS = List.of("NAME", "DESCRIPTION", "TYPE", "LABELS");

    private final Registry registry;
    private final OutputOptions outputOptions;

    public ProjectCollectionService(Registry registry, OutputOptions outputOptions) throws IOException {
        Assert.notNull(registry, "registry cannot be null");
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        this.registry = registry;
        this.outputOptions = outputOptions;
    }

    /**
     * Adds a collection to the registry and cache.
     */
    public void addCollection(String collectionName, String collectionRef) throws IOException {
        Assert.hasText(collectionName, "collectionName cannot be null or empty");
        Assert.hasText(collectionRef, "collectionRef cannot be null or empty");

        outputOptions.info("Adding collection '%s'...".formatted(collectionName));
        Index index = registry.getIndex(ContainerRef.parse(collectionRef));
        ProjectCollectionCache.save(collectionName, index);
        outputOptions.verbose("Collection '%s' saved to cache: %s".formatted(collectionName, ProjectCollectionCache.resolveCacheFileForCollection(collectionName)));
        ProjectCollectionRegistry.addCollection(ProjectCollectionRegistry.CollectionEntry.builder()
                        .name(collectionName)
                        .ref(collectionRef)
                        .digest(index.getDescriptor().getDigest())
                        .lastFetchedAt(Instant.now())
                        .build());
        outputOptions.verbose("Collection '%s' added to registry: %s.".formatted(collectionName, ProjectCollectionRegistry.registryPath()));
        outputOptions.info("Collection '%s' added.".formatted(collectionName));
        outputOptions.info("OCI Artifact: %s".formatted(collectionRef));
        outputOptions.info("Digest: %s".formatted(index.getDescriptor().getDigest()));
    }

    /**
     * Updates a collection in the registry and cache by re-fetching its OCI index.
     * Checks for a newer semver tag; if found, updates the stored ref to the latest version.
     */
    public void updateCollection(String collectionName) throws IOException {
        Assert.hasText(collectionName, "collectionName cannot be null or empty");

        ProjectCollectionRegistry.CollectionEntry existing = ProjectCollectionRegistry.findCollection(collectionName);
        if (existing == null) {
            outputOptions.error("Collection '%s' is not registered. Run 'arconia project collection add' to register it first.".formatted(collectionName));
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

        outputOptions.info("Updating collection '%s'...".formatted(collectionName));
        Index index = registry.getIndex(ContainerRef.parse(newRef));
        ProjectCollectionCache.save(collectionName, index);
        outputOptions.verbose("Collection '%s' saved to cache: %s".formatted(collectionName, ProjectCollectionCache.resolveCacheFileForCollection(collectionName)));
        ProjectCollectionRegistry.addCollection(ProjectCollectionRegistry.CollectionEntry.builder()
                .name(collectionName)
                .ref(newRef)
                .digest(index.getDescriptor().getDigest())
                .lastFetchedAt(Instant.now())
                .build());
        outputOptions.verbose("Collection '%s' updated in registry: %s.".formatted(collectionName, ProjectCollectionRegistry.registryPath()));
        outputOptions.info("Collection '%s' updated.".formatted(collectionName));
        outputOptions.info("OCI Artifact: %s".formatted(newRef));
        outputOptions.info("Digest: %s".formatted(index.getDescriptor().getDigest()));
    }

    /**
     * Removes a collection from the registry and cache.
     */
    public void removeCollection(String collectionName) throws IOException {
        Assert.hasText(collectionName, "collectionName cannot be null or empty");
        outputOptions.info("Removing collection '%s'...".formatted(collectionName));
        ProjectCollectionCache.delete(collectionName);
        ProjectCollectionRegistry.removeCollection(collectionName);
        outputOptions.info("Collection '%s' removed.".formatted(collectionName));
    }

    /**
     * Lists all projects in a collection.
     */
    public void listCollection(String collectionName) throws IOException {
        Assert.hasText(collectionName, "collectionName cannot be null or empty");

        var collection = ProjectCollectionRegistry.findCollection(collectionName);
        if (collection == null) {
            outputOptions.error("Collection '%s' is not registered. Run 'arconia project collection add' to register it first.".formatted(collectionName));
            return;
        }

        var index = ProjectCollectionCache.load(collectionName);
        if (index == null) {
            outputOptions.error("No cached data found for collection '%s'. Run 'arconia project collection add' to register it first.".formatted(collectionName));
            return;
        }

        var collectionCard = ProjectCollectionCard.builder()
                .header(ProjectCollectionHeader.builder()
                        .name(collection.name())
                        .ref(collection.ref())
                        .description(index.getAnnotations().get(ArtifactAnnotations.OCI_DESCRIPTION))
                        .build())
                .summaries(ProjectCollectionCache.toProjectSummaries(index))
                .build();

        printCollectionHeader(outputOptions, collectionCard.header());
        outputOptions.newLine();
        outputOptions.table(CliTableFormatter.format(outputOptions.colorScheme(), PROJECT_TABLE_HEADERS, toProjectRows(collectionCard.summaries()), 3));
    }

    private void printCollectionHeader(OutputOptions outputOptions, ProjectCollectionHeader header) {
        outputOptions.info("📦 %s (%s)".formatted(header.name(), header.ref()));
        if (StringUtils.hasText(header.description())) {
            outputOptions.info("   %s".formatted(header.description()));
        }
    }

    private List<List<String>> toProjectRows(List<ProjectCollectionSummary> summaries) {
        return summaries.stream()
                .map(s -> List.of(s.name(), s.description(), s.type(), String.join(", ", s.labels())))
                .toList();
    }

}
