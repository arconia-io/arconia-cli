package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SkillsManifest}.
 */
class SkillsManifestTests {

    @TempDir
    Path tempDir;

    // --- builder ---

    @Test
    void builderCreatesManifestWithNoSkills() {
        SkillsManifest manifest = SkillsManifest.builder().build();
        assertThat(manifest.skills()).isEmpty();
    }

    @Test
    void mutatePrePopulatesBuilderFromExistingManifest() {
        var entry = SkillsManifest.SkillEntry.builder()
            .name("skill")
            .source("ghcr.io/org/skill")
            .version("1.0.0")
            .build();

        SkillsManifest original = SkillsManifest.builder().build().addSkill(entry);
        SkillsManifest copy = original.mutate().build();

        assertThat(copy.skills()).hasSize(1);
        assertThat(copy.skills().getFirst().name()).isEqualTo("skill");
    }

    // --- load ---

    @Test
    void loadReturnsEmptyWhenFileDoesNotExist() throws IOException {
        SkillsManifest manifest = SkillsManifest.load(tempDir);
        assertThat(manifest.skills()).isEmpty();
    }

    @Test
    void loadParsesExistingFile() throws IOException {
        Files.writeString(tempDir.resolve(SkillsManifest.FILENAME), """
            {
              "skills": [
                {
                  "name": "pull-request",
                  "source": "ghcr.io/org/skills/pull-request",
                  "version": "1.2.0"
                }
              ]
            }
            """);

        SkillsManifest manifest = SkillsManifest.load(tempDir);

        assertThat(manifest.skills()).hasSize(1);
        assertThat(manifest.skills().getFirst().name()).isEqualTo("pull-request");
        assertThat(manifest.skills().getFirst().source()).isEqualTo("ghcr.io/org/skills/pull-request");
        assertThat(manifest.skills().getFirst().version()).isEqualTo("1.2.0");
    }

    // --- save ---

    @Test
    void saveWritesFileToProjectRoot() throws IOException {
        var entry = SkillsManifest.SkillEntry.builder()
            .name("test-skill")
            .source("ghcr.io/org/test-skill")
            .version("1.0.0")
            .build();

        SkillsManifest manifest = SkillsManifest.builder().build().addSkill(entry);
        manifest.save(tempDir);

        Path manifestFile = tempDir.resolve(SkillsManifest.FILENAME);
        assertThat(manifestFile).exists();
        String content = Files.readString(manifestFile);
        assertThat(content).contains("test-skill");
        assertThat(content).contains("ghcr.io/org/test-skill");
    }

    @Test
    void saveAndLoadRoundTrip() throws IOException {
        var entry = SkillsManifest.SkillEntry.builder()
            .name("my-skill")
            .source("ghcr.io/org/my-skill")
            .version("2.0.0")
            .build();

        SkillsManifest original = SkillsManifest.builder().build().addSkill(entry);
        original.save(tempDir);
        SkillsManifest loaded = SkillsManifest.load(tempDir);

        assertThat(loaded.skills()).hasSize(1);
        assertThat(loaded.skills().getFirst().name()).isEqualTo("my-skill");
        assertThat(loaded.skills().getFirst().source()).isEqualTo("ghcr.io/org/my-skill");
        assertThat(loaded.skills().getFirst().version()).isEqualTo("2.0.0");
    }

    // --- addSkill ---

    @Test
    void addSkillAppendsToEmptyManifest() {
        var entry = SkillsManifest.SkillEntry.builder()
            .name("new-skill")
            .source("ghcr.io/org/new-skill")
            .version("1.0.0")
            .build();

        SkillsManifest manifest = SkillsManifest.builder().build().addSkill(entry);

        assertThat(manifest.skills()).hasSize(1);
        assertThat(manifest.skills().getFirst().name()).isEqualTo("new-skill");
    }

    @Test
    void addSkillReplacesExistingEntryWithSameName() {
        var entry1 = SkillsManifest.SkillEntry.builder()
            .name("skill")
            .source("ghcr.io/org/skill")
            .version("1.0.0")
            .build();
        var entry2 = entry1.mutate().version("2.0.0").build();

        SkillsManifest manifest = SkillsManifest.builder().build()
            .addSkill(entry1)
            .addSkill(entry2);

        assertThat(manifest.skills()).hasSize(1);
        assertThat(manifest.skills().getFirst().version()).isEqualTo("2.0.0");
    }

    // --- removeSkill ---

    @Test
    void removeSkillRemovesExistingEntry() {
        var entry = SkillsManifest.SkillEntry.builder()
            .name("to-remove")
            .source("ghcr.io/org/to-remove")
            .version("1.0.0")
            .build();

        SkillsManifest manifest = SkillsManifest.builder().build()
            .addSkill(entry)
            .removeSkill("to-remove");

        assertThat(manifest.skills()).isEmpty();
    }

    @Test
    void removeSkillLeavesOtherEntries() {
        var keep = SkillsManifest.SkillEntry.builder()
            .name("keep")
            .source("ghcr.io/org/keep")
            .version("1.0.0")
            .build();
        var remove = SkillsManifest.SkillEntry.builder()
            .name("remove")
            .source("ghcr.io/org/remove")
            .version("1.0.0")
            .build();

        SkillsManifest manifest = SkillsManifest.builder().build()
            .addSkill(keep)
            .addSkill(remove)
            .removeSkill("remove");

        assertThat(manifest.skills()).hasSize(1);
        assertThat(manifest.skills().getFirst().name()).isEqualTo("keep");
    }

    @Test
    void removeSkillIsNoOpWhenNotFound() {
        var entry = SkillsManifest.SkillEntry.builder()
            .name("existing")
            .source("ghcr.io/org/existing")
            .version("1.0.0")
            .build();

        SkillsManifest manifest = SkillsManifest.builder().build()
            .addSkill(entry)
            .removeSkill("nonexistent");

        assertThat(manifest.skills()).hasSize(1);
    }

    // --- findSkill ---

    @Test
    void findSkillReturnsMatchingEntry() {
        var entry = SkillsManifest.SkillEntry.builder()
            .name("target")
            .source("ghcr.io/org/target")
            .version("1.0.0")
            .build();

        SkillsManifest manifest = SkillsManifest.builder().build().addSkill(entry);

        assertThat(manifest.findSkill("target")).isNotNull();
        assertThat(manifest.findSkill("target").version()).isEqualTo("1.0.0");
    }

    @Test
    void findSkillReturnsNullWhenNotFound() {
        SkillsManifest manifest = SkillsManifest.builder().build();
        assertThat(manifest.findSkill("missing")).isNull();
    }

    // --- additionalBasePaths ---

    @Test
    void builderSupportsAdditionalBasePaths() {
        var entry = SkillsManifest.SkillEntry.builder()
            .name("skill")
            .source("ghcr.io/org/skill")
            .version("1.0.0")
            .additionalBasePaths(List.of(".claude/skills", ".vibe/skills"))
            .build();

        assertThat(entry.additionalBasePaths()).containsExactly(".claude/skills", ".vibe/skills");
    }

    @Test
    void additionalBasePathsIsNullByDefault() {
        var entry = SkillsManifest.SkillEntry.builder()
            .name("skill")
            .source("ghcr.io/org/skill")
            .version("1.0.0")
            .build();

        assertThat(entry.additionalBasePaths()).isNull();
    }

    @Test
    void loadParsesAdditionalBasePaths() throws IOException {
        Files.writeString(tempDir.resolve(SkillsManifest.FILENAME), """
            {
              "skills": [
                {
                  "name": "pull-request",
                  "source": "ghcr.io/org/skills/pull-request",
                  "version": "1.2.0",
                  "additionalBasePaths": [".claude/skills", ".vibe/skills"]
                }
              ]
            }
            """);

        SkillsManifest manifest = SkillsManifest.load(tempDir);

        assertThat(manifest.skills().getFirst().additionalBasePaths())
            .containsExactly(".claude/skills", ".vibe/skills");
    }

    @Test
    void saveAndLoadRoundTripWithAdditionalBasePaths() throws IOException {
        var entry = SkillsManifest.SkillEntry.builder()
            .name("my-skill")
            .source("ghcr.io/org/my-skill")
            .version("2.0.0")
            .additionalBasePaths(List.of(".claude/skills"))
            .build();

        SkillsManifest original = SkillsManifest.builder().build().addSkill(entry);
        original.save(tempDir);
        SkillsManifest loaded = SkillsManifest.load(tempDir);

        assertThat(loaded.skills().getFirst().additionalBasePaths())
            .containsExactly(".claude/skills");
    }

    @Test
    void mutatePreservesAdditionalBasePaths() {
        var entry = SkillsManifest.SkillEntry.builder()
            .name("skill")
            .source("ghcr.io/org/skill")
            .version("1.0.0")
            .additionalBasePaths(List.of(".claude/skills"))
            .build();

        var mutated = entry.mutate().version("2.0.0").build();

        assertThat(mutated.version()).isEqualTo("2.0.0");
        assertThat(mutated.additionalBasePaths()).containsExactly(".claude/skills");
    }

}
