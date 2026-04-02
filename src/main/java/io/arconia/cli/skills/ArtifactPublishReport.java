package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.utils.JsonUtils;

/**
 * Model for an OCI artifact publish report (defaults to {@code publish-report.json}).
 * <p>
 * This report is optionally produced by publish commands such as
 * {@code skills push} and {@code skills collection push}, and records every
 * OCI artifact that was published during the operation. It serves as a
 * structured record of the published artifact(s) including their immutable
 * digests, enabling downstream operations such as:
 * <ul>
 *   <li>Collection pushing via {@code skills collection push --from-report}</li>
 *   <li>Artifact signing (e.g., cosign)</li>
 *   <li>SLSA provenance attestation</li>
 * </ul>
 *
 * @param publishedAt ISO 8601 timestamp of when the publish completed
 * @param artifacts the list of published artifact entries
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArtifactPublishReport(
    String publishedAt,
    List<ArtifactEntry> artifacts
) {

    /**
     * Default filename for the skills publish report.
     */
    public static final String DEFAULT_SKILLS_FILENAME = "publish-skills-report.json";

    /**
     * Default filename for the skills collection publish report.
     */
    public static final String DEFAULT_COLLECTION_FILENAME = "publish-skills-collection-report.json";

    public ArtifactPublishReport {
        Assert.hasText(publishedAt, "publishedAt cannot be null or empty");
        Assert.notNull(artifacts, "artifacts cannot be null");
    }

    /**
     * A single artifact entry in the publish report.
     *
     * @param name the artifact name (e.g., skill name from SKILL.md frontmatter, or collection name)
     * @param ref the full OCI artifact reference with tag (e.g., {@code ghcr.io/org/skills/pull-request:1.2.0})
     * @param version the semver version that was published
     * @param digest the immutable manifest digest
     * @param description the artifact description, may be {@code null}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtifactEntry(
        String name,
        String ref,
        String version,
        String digest,
        @Nullable String description
    ) {

        public ArtifactEntry {
            Assert.hasText(name, "name cannot be empty");
            Assert.hasText(ref, "ref cannot be empty");
            Assert.hasText(version, "version cannot be empty");
            Assert.hasText(digest, "digest cannot be empty");
        }

    }

    /**
     * Creates an empty report with the given timestamp.
     *
     * @param publishedAt ISO 8601 timestamp
     * @return an empty publish report
     */
    public static ArtifactPublishReport empty(String publishedAt) {
        return new ArtifactPublishReport(publishedAt, new ArrayList<>());
    }

    /**
     * Returns a new report with the given artifact entry appended.
     *
     * @param entry the artifact entry to add
     * @return a new report with the entry added
     */
    public ArtifactPublishReport withArtifact(ArtifactEntry entry) {
        Assert.notNull(entry, "entry cannot be null");

        List<ArtifactEntry> updated = new ArrayList<>(artifacts);
        updated.add(entry);
        return new ArtifactPublishReport(publishedAt, updated);
    }

    /**
     * Saves this report to the given file path.
     *
     * @param path the file path to write to
     * @throws IOException if the file cannot be written
     */
    public void save(Path path) throws IOException {
        Assert.notNull(path, "path cannot be null");

        String json = JsonUtils.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        Files.writeString(path, json + "\n");
    }

    /**
     * Loads a publish report from the given file path.
     *
     * @param path the file path to read from
     * @return the loaded publish report
     * @throws IOException if the file cannot be read
     */
    public static ArtifactPublishReport load(Path path) throws IOException {
        Assert.notNull(path, "path cannot be null");

        String json = Files.readString(path);
        return JsonUtils.getJsonMapper().readValue(json, ArtifactPublishReport.class);
    }

}
