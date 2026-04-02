package io.arconia.cli.skills;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AgentVendor}.
 */
class AgentVendorTests {

    // --- resolve ---

    @Test
    void resolvesClaude() {
        assertThat(AgentVendor.resolve("claude")).isEqualTo(AgentVendor.CLAUDE);
        assertThat(AgentVendor.resolve("claude-code")).isEqualTo(AgentVendor.CLAUDE);
    }

    @Test
    void resolvesVibe() {
        assertThat(AgentVendor.resolve("vibe")).isEqualTo(AgentVendor.VIBE);
        assertThat(AgentVendor.resolve("mistral")).isEqualTo(AgentVendor.VIBE);
        assertThat(AgentVendor.resolve("mistral-vibe")).isEqualTo(AgentVendor.VIBE);
    }

    @Test
    void resolvesContinue() {
        assertThat(AgentVendor.resolve("continue")).isEqualTo(AgentVendor.CONTINUE);
    }

    @Test
    void resolvesBob() {
        assertThat(AgentVendor.resolve("bob")).isEqualTo(AgentVendor.BOB);
        assertThat(AgentVendor.resolve("ibm-bob")).isEqualTo(AgentVendor.BOB);
    }

    @Test
    void resolveIsCaseInsensitive() {
        assertThat(AgentVendor.resolve("Claude")).isEqualTo(AgentVendor.CLAUDE);
        assertThat(AgentVendor.resolve("VIBE")).isEqualTo(AgentVendor.VIBE);
    }

    @Test
    void resolveTrimsWhitespace() {
        assertThat(AgentVendor.resolve("  claude  ")).isEqualTo(AgentVendor.CLAUDE);
    }

    @Test
    void resolveThrowsOnUnknownAlias() {
        assertThatThrownBy(() -> AgentVendor.resolve("unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown agent vendor 'unknown'");
    }

    @Test
    void resolveThrowsOnEmptyAlias() {
        assertThatThrownBy(() -> AgentVendor.resolve(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // --- skillsBasePath ---

    @Test
    void skillsBasePathReturnsCorrectPaths() {
        assertThat(AgentVendor.CLAUDE.skillsBasePath()).isEqualTo(".claude/skills");
        assertThat(AgentVendor.VIBE.skillsBasePath()).isEqualTo(".vibe/skills");
        assertThat(AgentVendor.CONTINUE.skillsBasePath()).isEqualTo(".continue/skills");
        assertThat(AgentVendor.BOB.skillsBasePath()).isEqualTo(".bob/skills");
    }

    // --- resolveBasePaths ---

    @Test
    void resolveBasePathsResolvesMultipleAliases() {
        List<String> paths = AgentVendor.resolveBasePaths(List.of("claude", "vibe"));
        assertThat(paths).containsExactly(".claude/skills", ".vibe/skills");
    }

    @Test
    void resolveBasePathsDeduplicates() {
        List<String> paths = AgentVendor.resolveBasePaths(List.of("claude", "claude-code"));
        assertThat(paths).containsExactly(".claude/skills");
    }

    @Test
    void resolveBasePathsDeduplicatesVibeAliases() {
        List<String> paths = AgentVendor.resolveBasePaths(List.of("vibe", "mistral"));
        assertThat(paths).containsExactly(".vibe/skills");
    }

    @Test
    void resolveBasePathsReturnsEmptyForEmptyInput() {
        List<String> paths = AgentVendor.resolveBasePaths(List.of());
        assertThat(paths).isEmpty();
    }

    @Test
    void resolveBasePathsThrowsOnUnknownAlias() {
        assertThatThrownBy(() -> AgentVendor.resolveBasePaths(List.of("claude", "unknown")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown agent vendor 'unknown'");
    }

    // --- supportedAliases ---

    @Test
    void supportedAliasesContainsAllAliases() {
        String aliases = AgentVendor.supportedAliases();
        assertThat(aliases).contains("claude", "claude-code");
        assertThat(aliases).contains("vibe", "mistral", "mistral-vibe");
        assertThat(aliases).contains("continue");
        assertThat(aliases).contains("bob", "ibm-bob");
    }

}
