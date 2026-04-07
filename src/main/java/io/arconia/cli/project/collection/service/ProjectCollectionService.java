package io.arconia.cli.project.collection.service;

import java.io.IOException;
import java.time.Instant;

import land.oras.ContainerRef;
import land.oras.Index;
import land.oras.Registry;

import io.arconia.cli.commands.options.OutputOptions;

import org.springframework.util.Assert;

/**
 * Service for managing project collections.
 */
public final class ProjectCollectionService {

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
     * Removes a collection from the registry and cache.
     */
    public void removeCollection(String collectionName) throws IOException {
        Assert.hasText(collectionName, "collectionName cannot be null or empty");
        outputOptions.info("Removing collection '%s'...".formatted(collectionName));
        ProjectCollectionCache.delete(collectionName);
        ProjectCollectionRegistry.removeCollection(collectionName);
        outputOptions.info("Collection '%s' removed.".formatted(collectionName));
    }


}
