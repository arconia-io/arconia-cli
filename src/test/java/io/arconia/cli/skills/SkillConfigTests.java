package io.arconia.cli.skills;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link SkillConfig}.
 */
class SkillConfigTests {

    @Test
    void builderCreatesConfigWithCurrentSchemaVersion() {
        SkillConfig config = SkillConfig.builder()
            .name("my-skill")
            .version("1.0.0")
            .description("A test skill")
            .build();

        assertThat(config.schemaVersion()).isEqualTo(SkillConfig.CURRENT_SCHEMA_VERSION);
        assertThat(config.name()).isEqualTo("my-skill");
        assertThat(config.version()).isEqualTo("1.0.0");
        assertThat(config.description()).isEqualTo("A test skill");
        assertThat(config.license()).isNull();
        assertThat(config.compatibility()).isNull();
        assertThat(config.allowedTools()).isNull();
        assertThat(config.metadata()).isNull();
    }

    @Test
    void builderWithNullVersionAndDescription() {
        SkillConfig config = SkillConfig.builder()
            .name("my-skill")
            .build();

        assertThat(config.name()).isEqualTo("my-skill");
        assertThat(config.version()).isNull();
        assertThat(config.description()).isNull();
    }

    @Test
    void builderThrowsWhenNameIsMissing() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillConfig.builder().build())
            .withMessageContaining("name");
    }

    @Test
    void mutateReturnsPrePopulatedBuilder() {
        SkillConfig original = SkillConfig.builder()
            .name("my-skill")
            .version("1.0.0")
            .description("Original")
            .license("MIT")
            .build();

        SkillConfig modified = original.mutate()
            .version("2.0.0")
            .description("Modified")
            .build();

        assertThat(modified.name()).isEqualTo("my-skill");
        assertThat(modified.version()).isEqualTo("2.0.0");
        assertThat(modified.description()).isEqualTo("Modified");
        assertThat(modified.license()).isEqualTo("MIT");
    }

    @Test
    void fromFrontmatterWithNullVersionLeavesVersionNull() {
        var frontmatter = new SkillFrontmatter(
            "pull-request", "Creates and manages PRs.",
            null, null, null, null);

        SkillConfig config = SkillConfig.fromFrontmatter(frontmatter, null);

        assertThat(config.name()).isEqualTo("pull-request");
        assertThat(config.description()).isEqualTo("Creates and manages PRs.");
        assertThat(config.version()).isNull();
    }

    @Test
    void fromFrontmatterWithExplicitVersion() {
        var frontmatter = new SkillFrontmatter(
            "pull-request", "Creates and manages PRs.",
            null, null, null, null);

        SkillConfig config = SkillConfig.fromFrontmatter(frontmatter, "1.2.0");

        assertThat(config.name()).isEqualTo("pull-request");
        assertThat(config.version()).isEqualTo("1.2.0");
    }

    @Test
    void fromFrontmatterIncludesAllFields() {
        var frontmatter = new SkillFrontmatter(
            "my-skill", "Does things.",
            "MIT", "Requires git", "Bash(git:*) Bash(gh:*)",
            Map.of("author", "arconia-io"));

        SkillConfig config = SkillConfig.fromFrontmatter(frontmatter, "1.2.0");

        assertThat(config.name()).isEqualTo("my-skill");
        assertThat(config.version()).isEqualTo("1.2.0");
        assertThat(config.description()).isEqualTo("Does things.");
        assertThat(config.license()).isEqualTo("MIT");
        assertThat(config.compatibility()).isEqualTo("Requires git");
        assertThat(config.allowedTools()).containsExactly("Bash(git:*)", "Bash(gh:*)");
        assertThat(config.metadata()).containsEntry("author", "arconia-io");
        assertThat(config.schemaVersion()).isEqualTo(SkillConfig.CURRENT_SCHEMA_VERSION);
    }

    @Test
    void fromFrontmatterThrowsWhenNameIsMissing() {
        var frontmatter = new SkillFrontmatter(
            null, "A skill without a name.",
            null, null, null, null);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillConfig.fromFrontmatter(frontmatter, null))
            .withMessageContaining("name");
    }

    @Test
    void fromFrontmatterThrowsWhenNameIsBlank() {
        var frontmatter = new SkillFrontmatter(
            "   ", "A skill with blank name.",
            null, null, null, null);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillConfig.fromFrontmatter(frontmatter, null))
            .withMessageContaining("name");
    }

    @Test
    void fromFrontmatterHandlesMissingOptionalFields() {
        var frontmatter = new SkillFrontmatter(
            "minimal-skill", null,
            null, null, null, null);

        SkillConfig config = SkillConfig.fromFrontmatter(frontmatter, null);

        assertThat(config.name()).isEqualTo("minimal-skill");
        assertThat(config.version()).isNull();
        assertThat(config.description()).isNull();
        assertThat(config.license()).isNull();
        assertThat(config.allowedTools()).isNull();
        assertThat(config.metadata()).isNull();
    }

}
