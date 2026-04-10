package io.arconia.cli.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.util.Assert;

import land.oras.Index;
import land.oras.Registry;

import io.arconia.cli.artifact.ArtifactDownloader;
import io.arconia.cli.commands.options.OutputOptions;
import io.arconia.cli.core.CliException;
import io.arconia.cli.openrewrite.OpenRewriteRunner;
import io.arconia.cli.openrewrite.RewriteArguments;
import io.arconia.cli.project.catalog.service.ProjectCatalogCache;
import io.arconia.cli.project.catalog.service.ProjectCatalogRegistry;
import io.arconia.cli.project.catalog.service.ProjectCatalogService;
import io.arconia.cli.project.catalog.service.ProjectCatalogSummary;
import io.arconia.cli.project.oci.ProjectConfigParser;
import io.arconia.cli.utils.IoUtils;

/**
 * Creates a new project from a template.
 */
public final class ProjectCreator {

    private final Registry registry;
    private final OutputOptions outputOptions;

    public ProjectCreator(Registry registry, OutputOptions outputOptions) {
        Assert.notNull(registry, "registry cannot be null");
        Assert.notNull(outputOptions, "outputOptions cannot be null");

        this.registry = registry;
        this.outputOptions = outputOptions;
    }

    /**
     * Creates a new project from the given template.
     */
    public void create(ProjectCreateArguments arguments, String templateName, Path targetParentDirectory) throws IOException {
        Assert.notNull(arguments, "arguments cannot be null");
        Assert.hasText(templateName, "templateName cannot be null or empty");
        Assert.notNull(targetParentDirectory, "targetParentDirectory cannot be null");

        String templateRef = resolveTemplateRef(templateName);

        outputOptions.newLine();
        outputOptions.info("Creating project '%s' from template '%s'...".formatted(arguments.name(), templateName));

        ArtifactDownloader artifactDownloader = new ArtifactDownloader(registry);
        Path projectDirectory = artifactDownloader.download(arguments.name(), templateRef, targetParentDirectory);

        outputOptions.info("Project '%s' created successfully".formatted(arguments.name()));

        String templatePackageName = ProjectConfigParser.parseFromDirectory(projectDirectory).packageName();

        outputOptions.newLine();
        outputOptions.info("Applying template customization...");

        Path recipeTemplate = null;
        try {
            recipeTemplate = IoUtils.copyFileToTemp("recipes/project-customization.yml");
            String content = Files.readString(recipeTemplate);
            content = content
                    .replace("${{name}}", arguments.name())
                    .replace("${{description}}", arguments.description())
                    .replace("${{group}}", arguments.group())
                    .replace("${{oldPackageName}}", templatePackageName)
                    .replace("${{packageName}}", arguments.packageName());
            Files.writeString(recipeTemplate, content);

            OpenRewriteRunner openRewriteRunner = new OpenRewriteRunner(projectDirectory, outputOptions, List.of());
            openRewriteRunner.rewriteRun(RewriteArguments.builder()
                    .rewriteRecipeName("io.arconia.rewrite.CustomizeProjectTemplate")
                    .rewriteConfigFile(recipeTemplate)
                    .build());
        } catch (IOException ex) {
            throw new CliException("Failed to prepare OpenRewrite recipe" + ex.getMessage(), ex);
        } finally {
            if (recipeTemplate != null) {
                Files.deleteIfExists(recipeTemplate);
            }
        }

        String packageRoot = templatePackageName.split("\\.")[0];
        String oldPackageFolderMain = "src/main/java/%s".formatted(packageRoot);
        String oldPackageFolderTest = "src/test/java/%s".formatted(packageRoot);

        outputOptions.verbose("Removing intermediate files...");
        outputOptions.verbose("  - Empty directories under '%s'".formatted(oldPackageFolderMain));
        outputOptions.verbose("  - Empty directories under '%s'".formatted(oldPackageFolderTest));
        outputOptions.verbose("  - Template configuration file '%s'".formatted(ProjectConfigParser.CONFIG_FILE_NAME));

        IoUtils.deleteEmptyDirectoriesRecursively(projectDirectory.resolve(oldPackageFolderMain));
        IoUtils.deleteEmptyDirectoriesRecursively(projectDirectory.resolve(oldPackageFolderTest));
        Files.deleteIfExists(projectDirectory.resolve(ProjectConfigParser.CONFIG_FILE_NAME));

        outputOptions.info("Template customization applied successfully");
        outputOptions.newLine();

        outputOptions.info("Project '%s' is now ready".formatted(arguments.name()));
        outputOptions.info("Path: %s".formatted(projectDirectory.toString()));
    }

    private String resolveTemplateRef(String templateName) throws IOException {
        if (templateName.contains("/")) {
            return templateName;
        }

        ProjectCatalogService catalogService = new ProjectCatalogService(registry, outputOptions);
        catalogService.ensureBuiltInCatalogRegistered();

        ProjectCatalogRegistry catalogRegistry = ProjectCatalogRegistry.load();
        for (ProjectCatalogRegistry.CatalogEntry collection : catalogRegistry.catalogs()) {
            Index index = ProjectCatalogCache.load(collection.name());
            if (index == null) {
                continue;
            }
            for (ProjectCatalogSummary summary : ProjectCatalogCache.toProjectSummaries(index)) {
                if (templateName.equals(summary.name())) {
                    outputOptions.info("Template '%s' resolved to '%s'".formatted(templateName, summary.ref()));
                    return summary.ref();
                }
            }
        }
        return templateName;
    }

}
