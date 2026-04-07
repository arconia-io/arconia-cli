package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import io.arconia.cli.utils.IoUtils;

/**
 * Discovers and publishes multiple skill directories found under a parent path.
 * <p>
 * Each direct subdirectory containing a {@code SKILL.md} file is treated as a
 * skill directory. This class provides the batch-publish logic used by the
 * {@code skills push --all} CLI command.
 */
public final class SkillBatchPublisher {

    private final SkillPublisher publisher;

    /**
     * Creates a new batch publisher.
     *
     * @param publisher the single-skill publisher
     */
    public SkillBatchPublisher(SkillPublisher publisher) {
        Assert.notNull(publisher, "publisher cannot be null");
        this.publisher = publisher;
    }

    /**
     * Result of a single skill publish attempt within a batch operation.
     */
    public sealed interface BatchEntryResult {

        /**
         * A skill was published successfully.
         *
         * @param directoryName the directory name of the skill
         * @param publishResult the publish result
         * @param targetRef the target OCI reference
         */
        record Success(String directoryName, SkillPublisher.PublishResult publishResult, SkillRef targetRef) implements BatchEntryResult {}

        /**
         * A skill failed to publish.
         *
         * @param directoryName the directory name of the skill
         * @param errorMessage the error message
         */
        record Failure(String directoryName, String errorMessage) implements BatchEntryResult {}
    }

    /**
     * Result of a batch publish operation.
     *
     * @param results the per-skill results
     * @param totalCount the total number of skills discovered
     * @param successCount the number of successfully published skills
     */
    public record BatchPublishResult(
        List<BatchEntryResult> results,
        int totalCount,
        int successCount
    ) {
        public BatchPublishResult {
            Assert.notNull(results, "results cannot be null");
        }
    }

    /**
     * Discovers all skill directories under the given parent path and publishes each one.
     * <p>
     * The OCI reference for each skill is constructed as {@code <baseRef>/<directory-name>}.
     *
     * @param parentPath the parent directory containing skill subdirectories
     * @param baseRef the base OCI reference prefix (e.g., {@code ghcr.io/org/skills})
     * @param tag the semver version tag
     * @param additionalTags additional tags to apply (may be {@code null})
     * @param extraAnnotations extra annotations to include
     * @return the batch publish result
     * @throws IOException if the parent directory cannot be read
     * @throws IllegalArgumentException if the parent path is not a directory
     */
    public BatchPublishResult publishAll(Path parentPath, String baseRef, String tag,
                                          List<String> additionalTags,
                                          Map<String, String> extraAnnotations) throws IOException {
        Assert.notNull(parentPath, "parentPath cannot be null");
        Assert.hasText(baseRef, "baseRef cannot be null or empty");
        Assert.hasText(tag, "tag cannot be null or empty");
        Assert.notNull(extraAnnotations, "extraAnnotations cannot be null");

        if (!Files.isDirectory(parentPath)) {
            throw new IllegalArgumentException(
                "Path is not a directory: %s".formatted(parentPath));
        }

        List<Path> skillDirs = IoUtils.discoverSubDirectoriesWithFile(parentPath, SkillFrontmatterParser.SKILL_FILENAME);
        List<BatchEntryResult> results = new ArrayList<>();
        int successCount = 0;

        for (Path skillDir : skillDirs) {
            String dirName = skillDir.getFileName().toString();
            String fullRef = "%s/%s".formatted(baseRef, dirName);

            try {
                SkillRef targetRef = SkillRef.parse(fullRef).mutate().tag(tag).build();
                SkillPublisher.PublishResult publishResult = publisher.publish(skillDir, targetRef, extraAnnotations);

                // Apply additional tags
                applyAdditionalTags(publishResult, fullRef, additionalTags);

                results.add(new BatchEntryResult.Success(dirName, publishResult, targetRef));
                successCount++;
            }
            catch (Exception e) {
                results.add(new BatchEntryResult.Failure(dirName, e.getMessage()));
            }
        }

        return new BatchPublishResult(results, skillDirs.size(), successCount);
    }

    /**
     * Publishes a single skill directory and optionally applies additional tags.
     *
     * @param skillDirectory the path to the skill directory
     * @param skillRef the base OCI reference (without tag)
     * @param tag the semver version tag
     * @param additionalTags additional tags to apply (may be {@code null})
     * @param extraAnnotations extra annotations to include
     * @return the publish result
     * @throws IOException if the skill cannot be published
     */
    public SkillPublisher.PublishResult publishSingle(Path skillDirectory, String skillRef, String tag,
                                                       List<String> additionalTags,
                                                       Map<String, String> extraAnnotations) throws IOException {
        Assert.notNull(skillDirectory, "skillDirectory cannot be null");
        Assert.hasText(skillRef, "skillRef cannot be null or empty");
        Assert.hasText(tag, "tag cannot be null or empty");
        Assert.notNull(extraAnnotations, "extraAnnotations cannot be null");

        SkillRef targetRef = SkillRef.parse(skillRef).mutate().tag(tag).build();
        SkillPublisher.PublishResult result = publisher.publish(skillDirectory, targetRef, extraAnnotations);

        applyAdditionalTags(result, skillRef, additionalTags);

        return result;
    }

    /**
     * Applies additional tags to an already-published skill artifact.
     *
     * @param result the publish result
     * @param skillRef the base OCI reference
     * @param additionalTags the tags to apply (may be {@code null})
     */
    private void applyAdditionalTags(SkillPublisher.PublishResult result, String skillRef,
                                      List<String> additionalTags) {
        if (additionalTags == null) {
            return;
        }
        for (String additionalTag : additionalTags) {
            publisher.tag(result, additionalTag);
        }
    }

}
