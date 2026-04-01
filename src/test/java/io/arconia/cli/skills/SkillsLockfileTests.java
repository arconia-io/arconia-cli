package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.arconia.cli.skills.SkillsLockfile.LockfileEntry;
import io.arconia.cli.skills.SkillsLockfile.Source;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SkillsLockfile}.
 */
class SkillsLockfileTests {

    @TempDir
    Path tempDir;

    // --- builder ---

    @Test
    void builderCreatesLockfileWithDefaults() {
        SkillsLockfile lockfile = SkillsLockfile.builder().build();

        assertThat(lockfile.lockfileVersion()).isEqualTo(SkillsLockfile.CURRENT_VERSION);
        assertThat(lockfile.skills()).isEmpty();
        assertThat(lockfile.generatedAt()).isNotNull();
    }

    @Test
    void mutatePrePopulatesBuilderFromExistingLockfile() {
        var entry = testEntry("skill", "sha256:abc");
        SkillsLockfile original = SkillsLockfile.builder().build().addOrUpdateEntry(entry);
        SkillsLockfile copy = original.mutate().build();

        assertThat(copy.lockfileVersion()).isEqualTo(original.lockfileVersion());
        assertThat(copy.generatedAt()).isEqualTo(original.generatedAt());
        assertThat(copy.skills()).hasSize(1);
        assertThat(copy.skills().getFirst().name()).isEqualTo("skill");
    }

    // --- load ---

    @Test
    void loadReturnsEmptyWhenFileDoesNotExist() throws IOException {
        SkillsLockfile lockfile = SkillsLockfile.load(tempDir);

        assertThat(lockfile.skills()).isEmpty();
    }

    @Test
    void loadParsesExistingFile() throws IOException {
        Files.writeString(tempDir.resolve(SkillsLockfile.FILENAME), """
            {
              "lockfileVersion": 1,
              "generatedAt": "2025-06-01T14:32:00Z",
              "skills": [
                {
                  "name": "pull-request",
                  "path": ".agents/skills/pull-request",
                  "source": {
                    "registry": "ghcr.io",
                    "repository": "org/skills/pull-request",
                    "tag": "1.2.0",
                    "digest": "sha256:abc123",
                    "ref": "ghcr.io/org/skills/pull-request:1.2.0@sha256:abc123"
                  },
                  "installedAt": "2025-06-01T14:32:00Z"
                }
              ]
            }
            """);

        SkillsLockfile lockfile = SkillsLockfile.load(tempDir);

        assertThat(lockfile.lockfileVersion()).isEqualTo(1);
        assertThat(lockfile.skills()).hasSize(1);

        var entry = lockfile.skills().getFirst();
        assertThat(entry.name()).isEqualTo("pull-request");
        assertThat(entry.path()).isEqualTo(".agents/skills/pull-request");
        assertThat(entry.source().registry()).isEqualTo("ghcr.io");
        assertThat(entry.source().repository()).isEqualTo("org/skills/pull-request");
        assertThat(entry.source().tag()).isEqualTo("1.2.0");
        assertThat(entry.source().digest()).isEqualTo("sha256:abc123");
    }

    // --- save ---

    @Test
    void saveWritesFileToProjectRoot() throws IOException {
        var entry = testEntry("test-skill", "sha256:abc");

        SkillsLockfile lockfile = SkillsLockfile.builder().build().addOrUpdateEntry(entry);
        lockfile.save(tempDir);

        Path lockfilePath = tempDir.resolve(SkillsLockfile.FILENAME);
        assertThat(lockfilePath).exists();

        String content = Files.readString(lockfilePath);
        assertThat(content).contains("test-skill");
        assertThat(content).contains("sha256:abc");
    }

    @Test
    void saveAndLoadRoundTrip() throws IOException {
        var source = Source.builder()
            .registry("ghcr.io")
            .repository("org/my-skill")
            .tag("2.0.0")
            .digest("sha256:def456")
            .ref("ghcr.io/org/my-skill:2.0.0@sha256:def456")
            .build();
        var entry = LockfileEntry.builder()
            .name("my-skill")
            .path(".agents/skills/my-skill")
            .source(source)
            .installedAt("2025-06-01T12:00:00Z")
            .build();

        SkillsLockfile original = SkillsLockfile.builder().build().addOrUpdateEntry(entry);
        original.save(tempDir);
        SkillsLockfile loaded = SkillsLockfile.load(tempDir);

        assertThat(loaded.skills()).hasSize(1);
        var loadedEntry = loaded.skills().getFirst();
        assertThat(loadedEntry.name()).isEqualTo("my-skill");
        assertThat(loadedEntry.source().digest()).isEqualTo("sha256:def456");
    }

    // --- addOrUpdateEntry ---

    @Test
    void addOrUpdateEntryAppendsNewEntry() {
        var entry = testEntry("new-skill", "sha256:aaa");

        SkillsLockfile lockfile = SkillsLockfile.builder().build().addOrUpdateEntry(entry);

        assertThat(lockfile.skills()).hasSize(1);
        assertThat(lockfile.skills().getFirst().name()).isEqualTo("new-skill");
    }

    @Test
    void addOrUpdateEntryReplacesExistingByName() {
        var entry1 = testEntry("skill", "sha256:old");
        var entry2 = entry1.mutate()
            .source(entry1.source().mutate().digest("sha256:new").build())
            .build();

        SkillsLockfile lockfile = SkillsLockfile.builder().build()
            .addOrUpdateEntry(entry1)
            .addOrUpdateEntry(entry2);

        assertThat(lockfile.skills()).hasSize(1);
        assertThat(lockfile.skills().getFirst().source().digest()).isEqualTo("sha256:new");
    }

    @Test
    void addOrUpdateEntryUpdatesGeneratedAt() throws InterruptedException {
        var entry = testEntry("skill", "sha256:aaa");

        SkillsLockfile original = SkillsLockfile.builder().build();
        String originalTimestamp = original.generatedAt();

        // Ensure timestamp changes
        Thread.sleep(10);
        SkillsLockfile updated = original.addOrUpdateEntry(entry);

        assertThat(updated.generatedAt()).isNotEqualTo(originalTimestamp);
    }

    // --- removeEntry ---

    @Test
    void removeEntryRemovesExisting() {
        var entry = testEntry("to-remove", "sha256:aaa");

        SkillsLockfile lockfile = SkillsLockfile.builder().build()
            .addOrUpdateEntry(entry)
            .removeEntry("to-remove");

        assertThat(lockfile.skills()).isEmpty();
    }

    @Test
    void removeEntryLeavesOtherEntries() {
        var keep = testEntry("keep", "sha256:aaa");
        var remove = testEntry("remove", "sha256:bbb");

        SkillsLockfile lockfile = SkillsLockfile.builder().build()
            .addOrUpdateEntry(keep)
            .addOrUpdateEntry(remove)
            .removeEntry("remove");

        assertThat(lockfile.skills()).hasSize(1);
        assertThat(lockfile.skills().getFirst().name()).isEqualTo("keep");
    }

    @Test
    void removeEntryIsNoOpWhenNotFound() {
        var entry = testEntry("existing", "sha256:aaa");

        SkillsLockfile lockfile = SkillsLockfile.builder().build()
            .addOrUpdateEntry(entry)
            .removeEntry("nonexistent");

        assertThat(lockfile.skills()).hasSize(1);
    }

    // --- findEntry ---

    @Test
    void findEntryReturnsMatchingEntry() {
        var entry = testEntry("target", "sha256:abc");

        SkillsLockfile lockfile = SkillsLockfile.builder().build().addOrUpdateEntry(entry);

        assertThat(lockfile.findEntry("target")).isNotNull();
        assertThat(lockfile.findEntry("target").source().digest()).isEqualTo("sha256:abc");
    }

    @Test
    void findEntryReturnsNullWhenNotFound() {
        SkillsLockfile lockfile = SkillsLockfile.builder().build();

        assertThat(lockfile.findEntry("missing")).isNull();
    }

    // --- helpers ---

    private static LockfileEntry testEntry(String name, String digest) {
        var source = Source.builder()
            .registry("ghcr.io")
            .repository("org/" + name)
            .tag("1.0.0")
            .digest(digest)
            .ref("ghcr.io/org/" + name + ":1.0.0@" + digest)
            .build();
        return LockfileEntry.builder()
            .name(name)
            .path(".agents/skills/" + name)
            .source(source)
            .installedAt("2025-01-01T00:00:00Z")
            .build();
    }

}
