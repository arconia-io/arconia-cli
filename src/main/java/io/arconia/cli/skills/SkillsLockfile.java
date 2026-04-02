package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.cli.utils.JsonUtils;

/**
 * The {@code skills.lock.json} lock file recording the exact installation state of every
 * agent skill installed in the project.
 * <p>
 * Maps directly to the fields defined by the Agent Skills Lockfile OpenAPI schema:
 * <ul>
 *   <li>{@code lockfileVersion} — required, lock file schema version (currently {@code 1})</li>
 *   <li>{@code generatedAt} — required, ISO 8601 timestamp of last generation</li>
 *   <li>{@code skills} — required, list of installed skill entries</li>
 * </ul>
 * <p>
 * Generated and updated automatically by the CLI; not intended for manual editing.
 * This is the machine-generated counterpart to {@link SkillsManifest}.
 *
 * @param lockfileVersion version of the lock file schema; clients MUST reject unknown versions
 * @param generatedAt ISO 8601 timestamp recording when this lock file was last generated or updated
 * @param skills list of installed skill entries
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsLockfile(
    int lockfileVersion,
    String generatedAt,
    List<LockfileEntry> skills
) {

    /**
     * The filename for the skills lock file.
     */
    public static final String FILENAME = "skills.lock.json";

    /**
     * Current lock file schema version.
     */
    public static final int CURRENT_VERSION = 1;

    public SkillsLockfile {
        Assert.hasText(generatedAt, "generatedAt must not be empty");
        Assert.notNull(skills, "skills must not be null");
    }

    /**
     * Creates a new builder for {@link SkillsLockfile}.
     * The builder defaults {@code lockfileVersion} to {@link #CURRENT_VERSION},
     * {@code generatedAt} to the current instant, and {@code skills} to an empty list.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder pre-populated with the values from this lock file,
     * allowing selective modification.
     *
     * @return a pre-populated builder
     */
    public Builder mutate() {
        return new Builder()
            .lockfileVersion(lockfileVersion)
            .generatedAt(generatedAt)
            .skills(new ArrayList<>(skills));
    }

    /**
     * Builder for {@link SkillsLockfile}.
     */
    public static class Builder {

        private int lockfileVersion = CURRENT_VERSION;
        private String generatedAt = Instant.now().toString();
        private List<LockfileEntry> skills = new ArrayList<>();

        private Builder() {}

        public Builder lockfileVersion(int lockfileVersion) {
            this.lockfileVersion = lockfileVersion;
            return this;
        }

        public Builder generatedAt(String generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        public Builder skills(List<LockfileEntry> skills) {
            this.skills = new ArrayList<>(skills);
            return this;
        }

        public SkillsLockfile build() {
            return new SkillsLockfile(lockfileVersion, generatedAt, skills);
        }

    }

    /**
     * Records the exact installation state of a single agent skill.
     * <p>
     * Maps directly to the fields defined by the Agent Skills Lockfile OpenAPI schema:
     * <ul>
     *   <li>{@code name} — required, skill name as declared in {@code SKILL.md}</li>
     *   <li>{@code path} — required, relative path to the primary extracted skill directory</li>
     *   <li>{@code additionalPaths} — optional, additional relative paths where the skill was copied</li>
     *   <li>{@code source} — required, OCI source information</li>
     *   <li>{@code installedAt} — required, ISO 8601 timestamp of installation</li>
     * </ul>
     *
     * @param name the skill name as declared in {@code SKILL.md}; used as the stable key
     *             for update checks and removal operations
     * @param path relative path (from the project root) to the primary directory where the skill was extracted
     *             (e.g. {@code .agents/skills/pull-request})
     * @param additionalPaths additional relative paths (from the project root) where the skill was copied
     *                        for vendor-specific agent tools (e.g. {@code .claude/skills/pull-request});
     *                        may be {@code null} or empty if no additional copies exist
     * @param source the OCI source information identifying the exact artifact that was installed
     * @param installedAt ISO 8601 timestamp recording when this skill was installed or last updated
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LockfileEntry(
        String name,
        String path,
        @Nullable List<String> additionalPaths,
        Source source,
        String installedAt
    ) {

        public LockfileEntry {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.hasText(path, "path cannot be null or empty");
            Assert.notNull(source, "source cannot be null");
            Assert.hasText(installedAt, "installedAt cannot be null or empty");
        }

        /**
         * Returns all paths where this skill is installed (primary + additional).
         *
         * @return all installation paths
         */
        public List<String> allPaths() {
            List<String> all = new ArrayList<>();
            all.add(path);
            if (additionalPaths != null) {
                all.addAll(additionalPaths);
            }
            return all;
        }

        /**
         * Creates a new builder for {@link LockfileEntry}.
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
                .path(path)
                .additionalPaths(additionalPaths != null ? new ArrayList<>(additionalPaths) : null)
                .source(source)
                .installedAt(installedAt);
        }

        /**
         * Builder for {@link LockfileEntry}.
         */
        public static class Builder {

            private String name;
            private String path;
            private @Nullable List<String> additionalPaths;
            private Source source;
            private String installedAt;

            private Builder() {}

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder path(String path) {
                this.path = path;
                return this;
            }

            public Builder additionalPaths(@Nullable List<String> additionalPaths) {
                this.additionalPaths = additionalPaths;
                return this;
            }

            public Builder source(Source source) {
                this.source = source;
                return this;
            }

            public Builder installedAt(String installedAt) {
                this.installedAt = installedAt;
                return this;
            }

            public LockfileEntry build() {
                return new LockfileEntry(name, path, additionalPaths, source, installedAt);
            }

        }

    }

    /**
     * OCI source information identifying the exact artifact pulled to install a skill.
     * <p>
     * Maps directly to the fields defined by the Agent Skills Lockfile OpenAPI schema:
     * <ul>
     *   <li>{@code registry} — required, OCI registry hostname</li>
     *   <li>{@code repository} — required, repository path within the registry</li>
     *   <li>{@code tag} — required, mutable tag resolved at install time</li>
     *   <li>{@code digest} — required, immutable manifest digest ({@code sha256:<64 hex chars>})</li>
     *   <li>{@code ref} — required, fully-qualified digest-pinned reference</li>
     * </ul>
     * <p>
     * The {@code digest} is the canonical identifier used for reproducibility and update checks.
     * The {@code ref} field is enough on its own to re-pull the exact same artifact
     * from any compatible registry client.
     *
     * @param registry OCI registry hostname (e.g. {@code ghcr.io})
     * @param repository repository path within the registry, excluding the hostname
     *                   (e.g. {@code arconia-io/agent-skills/pull-request})
     * @param tag the mutable OCI tag resolved at install time (e.g. {@code 1.2.0});
     *            used as the starting point for update checks
     * @param digest the immutable SHA-256 digest of the OCI manifest that was pulled;
     *               format: {@code sha256:<64 hex chars>}
     * @param ref the fully-qualified, digest-pinned OCI reference in the form
     *            {@code registry/repository:tag@digest}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(
        String registry,
        String repository,
        String tag,
        String digest,
        String ref
    ) {

        /**
         * Required prefix for digest values per the OCI specification.
         */
        private static final String DIGEST_PREFIX = "sha256:";

        public Source {
            Assert.hasText(registry, "registry cannot be null or empty");
            Assert.hasText(repository, "repository cannot be null or empty");
            Assert.hasText(tag, "tag cannot be null or empty");
            Assert.hasText(digest, "digest cannot be null or empty");
            if (!digest.startsWith(DIGEST_PREFIX)) {
                throw new IllegalArgumentException(
                    "Invalid digest format '%s'. Digest must start with '%s'.".formatted(digest, DIGEST_PREFIX));
            }
            Assert.hasText(ref, "ref cannot be null or empty");
        }

        /**
         * Creates a new builder for {@link Source}.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Returns a builder pre-populated with the values from this source,
         * allowing selective modification.
         *
         * @return a pre-populated builder
         */
        public Builder mutate() {
            return new Builder()
                .registry(registry)
                .repository(repository)
                .tag(tag)
                .digest(digest)
                .ref(ref);
        }

        /**
         * Builder for {@link Source}.
         */
        public static class Builder {

            private String registry;
            private String repository;
            private String tag;
            private String digest;
            private String ref;

            private Builder() {}

            public Builder registry(String registry) {
                this.registry = registry;
                return this;
            }

            public Builder repository(String repository) {
                this.repository = repository;
                return this;
            }

            public Builder tag(String tag) {
                this.tag = tag;
                return this;
            }

            public Builder digest(String digest) {
                this.digest = digest;
                return this;
            }

            public Builder ref(String ref) {
                this.ref = ref;
                return this;
            }

            public Source build() {
                return new Source(registry, repository, tag, digest, ref);
            }

        }

    }

    /**
     * Loads the lock file from the given project root directory.
     * Returns an empty lock file if the file does not exist.
     *
     * @param projectRoot the project root directory
     * @return the loaded lock file
     * @throws IOException if the file cannot be read
     */
    public static SkillsLockfile load(Path projectRoot) throws IOException {
        Assert.notNull(projectRoot, "projectRoot cannot be null");

        Path lockfilePath = projectRoot.resolve(FILENAME);
        if (!Files.exists(lockfilePath)) {
            return builder().build();
        }

        String json = Files.readString(lockfilePath);
        return JsonUtils.getJsonMapper().readValue(json, SkillsLockfile.class);
    }

    /**
     * Saves this lock file to the given project root directory.
     *
     * @param projectRoot the project root directory
     * @throws IOException if the file cannot be written
     */
    public void save(Path projectRoot) throws IOException {
        Assert.notNull(projectRoot, "projectRoot cannot be null");

        Path lockfilePath = projectRoot.resolve(FILENAME);
        String json = JsonUtils.getJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        Files.writeString(lockfilePath, json + "\n");
    }

    /**
     * Returns a new lock file with the given entry added or updated.
     * If an entry with the same name already exists, it is replaced.
     * The {@code generatedAt} timestamp is updated to the current time.
     *
     * @param entry the lock file entry to add or update
     * @return a new lock file with the entry added
     */
    public SkillsLockfile addOrUpdateEntry(LockfileEntry entry) {
        Assert.notNull(entry, "entry cannot be null");

        List<LockfileEntry> updated = new ArrayList<>(skills.stream()
            .filter(s -> !s.name().equals(entry.name()))
            .toList());
        updated.add(entry);
        return mutate()
            .generatedAt(Instant.now().toString())
            .skills(updated)
            .build();
    }

    /**
     * Returns a new lock file with the named entry removed.
     * The {@code generatedAt} timestamp is updated to the current time.
     *
     * @param name the skill name to remove
     * @return a new lock file without the named entry
     */
    public SkillsLockfile removeEntry(String name) {
        Assert.hasText(name, "name must not be empty");

        List<LockfileEntry> updated = new ArrayList<>(skills.stream()
            .filter(s -> !s.name().equals(name))
            .toList());
        return mutate()
            .generatedAt(Instant.now().toString())
            .skills(updated)
            .build();
    }

    /**
     * Finds a lock file entry by name.
     *
     * @param name the skill name
     * @return the matching entry, or {@code null} if not found
     */
    @Nullable
    public LockfileEntry findEntry(String name) {
        Assert.hasText(name, "name must not be empty");

        return skills.stream()
            .filter(s -> s.name().equals(name))
            .findFirst()
            .orElse(null);
    }

}
