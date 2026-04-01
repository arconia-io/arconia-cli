package io.arconia.cli.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link SkillFrontmatterParser}.
 */
class SkillFrontmatterParserTests {

    @TempDir
    Path tempDir;

    @Test
    void parseFromDirectoryExtractsStandardFrontmatter() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), """
            ---
            name: pull-request
            description: Creates and manages GitHub pull requests.
            license: Apache-2.0
            ---
            # Pull Request
            Body content here.
            """);

        SkillFrontmatter result = SkillFrontmatterParser.parseFromDirectory(tempDir);

        assertThat(result.name()).isEqualTo("pull-request");
        assertThat(result.description()).isEqualTo("Creates and manages GitHub pull requests.");
        assertThat(result.license()).isEqualTo("Apache-2.0");
    }

    @Test
    void parseFromDirectoryHandlesQuotedValues() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), """
            ---
            name: "my-skill"
            description: 'A quoted description'
            ---
            # Body
            """);

        SkillFrontmatter result = SkillFrontmatterParser.parseFromDirectory(tempDir);

        assertThat(result.name()).isEqualTo("my-skill");
        assertThat(result.description()).isEqualTo("A quoted description");
    }

    @Test
    void parseFromDirectorySkipsCommentsAndEmptyLines() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), """
            ---
            name: test-skill
            # This is a comment

            description: A test skill.
            ---
            """);

        SkillFrontmatter result = SkillFrontmatterParser.parseFromDirectory(tempDir);

        assertThat(result.name()).isEqualTo("test-skill");
        assertThat(result.description()).isEqualTo("A test skill.");
    }

    @Test
    void parseFromDirectoryHandlesAllowedTools() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), """
            ---
            name: git-skill
            description: Does git things.
            allowed-tools: "Bash(git:*) Bash(gh:*)"
            ---
            # Content
            """);

        SkillFrontmatter result = SkillFrontmatterParser.parseFromDirectory(tempDir);

        assertThat(result.name()).isEqualTo("git-skill");
        assertThat(result.allowedTools()).isEqualTo("Bash(git:*) Bash(gh:*)");
    }

    @Test
    void parseFromDirectoryHandlesMetadata() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), """
            ---
            name: my-skill
            description: A skill.
            metadata:
              author: arconia-io
              category: vcs
              version: "1.0"
            ---
            # Content
            """);

        SkillFrontmatter result = SkillFrontmatterParser.parseFromDirectory(tempDir);

        assertThat(result.name()).isEqualTo("my-skill");
        assertThat(result.metadata())
            .containsEntry("author", "arconia-io")
            .containsEntry("category", "vcs")
            .containsEntry("version", "1.0");
    }

    @Test
    void parseFromDirectoryHandlesCompatibility() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), """
            ---
            name: docker-skill
            description: Manages Docker containers.
            compatibility: Requires docker and internet access
            ---
            # Content
            """);

        SkillFrontmatter result = SkillFrontmatterParser.parseFromDirectory(tempDir);

        assertThat(result.compatibility()).isEqualTo("Requires docker and internet access");
    }

    @Test
    void parseFromDirectoryIgnoresUnknownFields() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), """
            ---
            name: my-skill
            description: A skill.
            version: 1.0.0
            slug: my-custom-slug
            custom-field: some-value
            ---
            # Content
            """);

        SkillFrontmatter result = SkillFrontmatterParser.parseFromDirectory(tempDir);

        assertThat(result.name()).isEqualTo("my-skill");
        assertThat(result.description()).isEqualTo("A skill.");
    }

    @Test
    void parseFromDirectoryThrowsWhenSkillMdMissing() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillFrontmatterParser.parseFromDirectory(tempDir))
            .withMessageContaining("SKILL.md");
    }

    @Test
    void parseFromDirectoryThrowsWhenNoFrontmatterDelimiter() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), "# Just a markdown file without frontmatter");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillFrontmatterParser.parseFromDirectory(tempDir))
            .withMessageContaining("---");
    }

    @Test
    void parseFromDirectoryThrowsWhenClosingDelimiterMissing() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), """
            ---
            name: broken
            description: Incomplete frontmatter.
            """);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillFrontmatterParser.parseFromDirectory(tempDir))
            .withMessageContaining("closing");
    }

    @Test
    void parseFromDirectoryThrowsWhenFileIsEmpty() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), "");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillFrontmatterParser.parseFromDirectory(tempDir));
    }

}
