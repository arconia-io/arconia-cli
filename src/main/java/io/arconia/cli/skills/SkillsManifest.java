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
 * The {@code skills.json} intent file declaring which agent skills a project requires.
 * <p>
 * Maps directly to the fields defined by the Agent Skills Manifest OpenAPI schema:
 * <ul>
 *   <li>{@code skills} — required, ordered list of skill entries</li>
 * </ul>
 * <p>
 * This is the user-edited, declarative counterpart to the machine-generated
 * {@link SkillsLockfile}. Analogous to {@code package.json} (npm) or {@code Cargo.toml} (Rust).
 * The file lives at the project root and is intended to be committed to version control.
 *
 * @param skills ordered list of agent skill entries required by this project
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsManifest(
    List<SkillEntry> skills
) {

    /**
     * The filename for the skills manifest.
     */
    public static final String FILENAME = "skills.json";

    public SkillsManifest {
        Assert.notNull(skills, "skills must not be null");
    }

    /**
     * Creates a new builder for {@link SkillsManifest}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder pre-populated with the values from this manifest,
     * allowing selective modification.
     *
     * @return a pre-populated builder
     */
    public Builder mutate() {
        return new Builder().skills(new ArrayList<>(skills));
    }

    /**
     * Builder for {@link SkillsManifest}.
     */
    public static class Builder {

        private List<SkillEntry> skills = new ArrayList<>();

        private Builder() {}

        public Builder skills(List<SkillEntry> skills) {
            this.skills = new ArrayList<>(skills);
            return this;
        }

        public SkillsManifest build() {
            return new SkillsManifest(skills);
        }

    }

    /**
     * A single agent skill declaration inside {@code skills.json}.
     * <p>
     * Maps directly to the fields defined by the Agent Skills Manifest OpenAPI schema:
     * <ul>
     *   <li>{@code name} — required, human-readable skill identifier</li>
     *   <li>{@code source} — required, OCI repository reference without tag or digest</li>
     *   <li>{@code version} — optional, OCI tag to install; SHOULD follow Semantic Versioning 2.0.0</li>
     *   <li>{@code additionalBasePaths} — optional, extra base directories to copy the skill into</li>
     * </ul>
     *
     * @param name human-readable skill identifier; SHOULD match the last path segment of {@code source}
     *             and the {@code name} field in the skill's {@code SKILL.md} frontmatter
     * @param source OCI repository reference for the skill artifact, without a tag or digest
     *               (e.g. {@code ghcr.io/arconia-io/agent-skills/pull-request})
     * @param version OCI tag to install (e.g. {@code 1.2.0}); when {@code null} the CLI resolves
     *                the tag at install time and records the resolved digest in {@code skills.lock.json}
     * @param additionalBasePaths extra base directories (e.g. {@code .claude/skills}, {@code .vibe/skills})
     *                            where the skill should also be copied; when {@code null} or empty, the skill
     *                            is only installed to the default {@code .agents/skills} directory
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkillEntry(
        String name,
        String source,
        @Nullable String version,
        @Nullable List<String> additionalBasePaths
    ) {

        public SkillEntry {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.hasText(source, "source cannot be null or empty");
        }

        /**
         * Creates a new builder for {@link SkillEntry}.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Returns a builder pre-populated with the values from this entry,
         * allowing selective modification.
         *
         * @return a pre-populated builder
         */
        public Builder mutate() {
            return new Builder()
                .name(name)
                .source(source)
                .version(version)
                .additionalBasePaths(additionalBasePaths != null ? new ArrayList<>(additionalBasePaths) : null);
        }

        /**
         * Builder for {@link SkillEntry}.
         */
        public static class Builder {

            private String name;
            private String source;
            private @Nullable String version;
            private @Nullable List<String> additionalBasePaths;

            private Builder() {}

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder source(String source) {
                this.source = source;
                return this;
            }

            public Builder version(@Nullable String version) {
                this.version = version;
                return this;
            }

            public Builder additionalBasePaths(@Nullable List<String> additionalBasePaths) {
                this.additionalBasePaths = additionalBasePaths;
                return this;
            }

            public SkillEntry build() {
                return new SkillEntry(name, source, version, additionalBasePaths);
            }

        }

    }

    /**
     * Loads the manifest from the given project root directory.
     * Returns an empty manifest if the file does not exist.
     *
     * @param projectRoot the project root directory
     * @return the loaded manifest
     * @throws IOException if the file cannot be read
     */
    public static SkillsManifest load(Path projectRoot) throws IOException {
        Assert.notNull(projectRoot, "projectRoot cannot be null");

        Path manifestPath = projectRoot.resolve(FILENAME);
        if (!Files.exists(manifestPath)) {
            return builder().build();
        }

        String json = Files.readString(manifestPath);
        return JsonUtils.getJsonMapper().readValue(json, SkillsManifest.class);
    }

    /**
     * Saves this manifest to the given project root directory.
     *
     * @param projectRoot the project root directory
     * @throws IOException if the file cannot be written
     */
    public void save(Path projectRoot) throws IOException {
        Assert.notNull(projectRoot, "projectRoot cannot be null");

        Path manifestPath = projectRoot.resolve(FILENAME);
        String json = JsonUtils.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        Files.writeString(manifestPath, json + "\n");
    }

    /**
     * Returns a new manifest with the given skill added or updated.
     * If a skill with the same name already exists, it is replaced.
     *
     * @param entry the skill entry to add
     * @return a new manifest with the skill added
     */
    public SkillsManifest addSkill(SkillEntry entry) {
        Assert.notNull(entry, "entry cannot be null");

        List<SkillEntry> updated = new ArrayList<>(skills.stream()
            .filter(s -> !s.name().equals(entry.name()))
            .toList());
        updated.add(entry);
        return builder().skills(updated).build();
    }

    /**
     * Returns a new manifest with the named skill removed.
     *
     * @param name the skill name to remove
     * @return a new manifest without the named skill
     */
    public SkillsManifest removeSkill(String name) {
        Assert.hasText(name, "name cannot be null or empty");

        List<SkillEntry> updated = new ArrayList<>(skills.stream()
            .filter(s -> !s.name().equals(name))
            .toList());
        return builder().skills(updated).build();
    }

    /**
     * Finds a skill entry by name.
     *
     * @param name the skill name
     * @return the matching entry, or {@code null} if not found
     */
    @Nullable
    public SkillEntry findSkill(String name) {
        Assert.hasText(name, "name cannot be null or empty");

        return skills.stream()
            .filter(s -> s.name().equals(name))
            .findFirst()
            .orElse(null);
    }

}
