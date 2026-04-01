package io.arconia.cli.skills;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link SkillRef}.
 */
class SkillRefTests {

    // --- parse ---

    @Test
    void parseFullReference() {
        SkillRef ref = SkillRef.parse("ghcr.io/arconia-io/agent-skills/pull-request:1.2.0");

        assertThat(ref.registry()).isEqualTo("ghcr.io");
        assertThat(ref.repository()).isEqualTo("arconia-io/agent-skills/pull-request");
        assertThat(ref.tag()).isEqualTo("1.2.0");
        assertThat(ref.digest()).isNull();
    }

    @Test
    void parseReferenceWithLatestTag() {
        SkillRef ref = SkillRef.parse("ghcr.io/arconia-io/agent-skills/pull-request:latest");

        assertThat(ref.registry()).isEqualTo("ghcr.io");
        assertThat(ref.tag()).isEqualTo("latest");
    }

    @Test
    void parseThrowsForEmptyReference() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillRef.parse(""));
    }

    @Test
    void parseThrowsForNullReference() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillRef.parse(null));
    }

    // --- skillName ---

    @Test
    void skillNameReturnsLastSegment() {
        SkillRef ref = SkillRef.parse("ghcr.io/arconia-io/agent-skills/pull-request:1.0.0");

        assertThat(ref.skillName()).isEqualTo("pull-request");
    }

    @Test
    void skillNameWithSingleSegmentRepository() {
        SkillRef ref = SkillRef.builder()
            .registry("ghcr.io")
            .repository("my-skill")
            .tag("1.0.0")
            .build();

        assertThat(ref.skillName()).isEqualTo("my-skill");
    }

    @Test
    void skillNameWithDeepRepository() {
        SkillRef ref = SkillRef.parse("ghcr.io/org/group/subgroup/skill-name:latest");

        assertThat(ref.skillName()).isEqualTo("skill-name");
    }

    // --- effectiveTag ---

    @Test
    void effectiveTagReturnsTagWhenPresent() {
        SkillRef ref = SkillRef.builder()
            .registry("ghcr.io")
            .repository("org/repo")
            .tag("1.0.0")
            .build();

        assertThat(ref.effectiveTag()).isEqualTo("1.0.0");
    }

    @Test
    void effectiveTagReturnsLatestWhenTagIsNull() {
        SkillRef ref = SkillRef.builder()
            .registry("ghcr.io")
            .repository("org/repo")
            .digest("sha256:abc123")
            .build();

        assertThat(ref.effectiveTag()).isEqualTo("latest");
    }

    // --- fullTagReference ---

    @Test
    void fullTagReferenceFormatsCorrectly() {
        SkillRef ref = SkillRef.parse("ghcr.io/arconia-io/agent-skills/pull-request:1.2.0");

        assertThat(ref.fullTagReference()).isEqualTo("ghcr.io/arconia-io/agent-skills/pull-request:1.2.0");
    }

    @Test
    void fullTagReferenceUsesLatestWhenNoTag() {
        SkillRef ref = SkillRef.builder()
            .registry("ghcr.io")
            .repository("org/skill")
            .digest("sha256:abc")
            .build();

        assertThat(ref.fullTagReference()).isEqualTo("ghcr.io/org/skill:latest");
    }

    // --- fullDigestReference ---

    @Test
    void fullDigestReferenceFormatsCorrectly() {
        SkillRef ref = SkillRef.builder()
            .registry("ghcr.io")
            .repository("org/skill")
            .tag("1.0.0")
            .digest("sha256:abc123")
            .build();

        assertThat(ref.fullDigestReference()).isEqualTo("ghcr.io/org/skill@sha256:abc123");
    }

    @Test
    void fullDigestReferenceReturnsNullWhenNoDigest() {
        SkillRef ref = SkillRef.builder()
            .registry("ghcr.io")
            .repository("org/skill")
            .tag("1.0.0")
            .build();

        assertThat(ref.fullDigestReference()).isNull();
    }

    // --- fullReference ---

    @Test
    void fullReferenceWithTagAndDigest() {
        SkillRef ref = SkillRef.builder()
            .registry("ghcr.io")
            .repository("org/skill")
            .tag("1.0.0")
            .digest("sha256:abc123")
            .build();

        assertThat(ref.fullReference()).isEqualTo("ghcr.io/org/skill:1.0.0@sha256:abc123");
    }

    @Test
    void fullReferenceWithTagOnly() {
        SkillRef ref = SkillRef.builder()
            .registry("ghcr.io")
            .repository("org/skill")
            .tag("1.0.0")
            .build();

        assertThat(ref.fullReference()).isEqualTo("ghcr.io/org/skill:1.0.0");
    }

    @Test
    void fullReferenceWithDigestOnlyOmitsTag() {
        SkillRef ref = SkillRef.builder()
            .registry("ghcr.io")
            .repository("org/skill")
            .digest("sha256:abc123")
            .build();

        assertThat(ref.fullReference()).isEqualTo("ghcr.io/org/skill@sha256:abc123");
    }

    // --- builder ---

    @Test
    void builderCreatesRef() {
        SkillRef ref = SkillRef.builder()
            .registry("ghcr.io")
            .repository("org/skill")
            .tag("1.0.0")
            .build();

        assertThat(ref.registry()).isEqualTo("ghcr.io");
        assertThat(ref.repository()).isEqualTo("org/skill");
        assertThat(ref.tag()).isEqualTo("1.0.0");
        assertThat(ref.digest()).isNull();
    }

    @Test
    void builderWithDigestOnly() {
        SkillRef ref = SkillRef.builder()
            .registry("ghcr.io")
            .repository("org/skill")
            .digest("sha256:abc123")
            .build();

        assertThat(ref.digest()).isEqualTo("sha256:abc123");
        assertThat(ref.tag()).isNull();
    }

    @Test
    void builderThrowsWhenRegistryIsEmpty() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillRef.builder()
                .registry("")
                .repository("repo")
                .tag("tag")
                .build());
    }

    @Test
    void builderThrowsWhenRepositoryIsEmpty() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillRef.builder()
                .registry("ghcr.io")
                .repository("")
                .tag("tag")
                .build());
    }

    @Test
    void builderThrowsWhenBothTagAndDigestAreAbsent() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> SkillRef.builder()
                .registry("ghcr.io")
                .repository("org/skill")
                .build());
    }

    // --- mutate ---

    @Test
    void mutatePrePopulatesBuilderFromExistingRef() {
        SkillRef ref = SkillRef.parse("ghcr.io/org/skill:1.0.0");
        SkillRef pinned = ref.mutate().digest("sha256:abc123").build();

        assertThat(pinned.digest()).isEqualTo("sha256:abc123");
        assertThat(ref.digest()).isNull();
        assertThat(pinned.tag()).isEqualTo("1.0.0");
        assertThat(pinned.registry()).isEqualTo(ref.registry());
        assertThat(pinned.repository()).isEqualTo(ref.repository());
    }

    @Test
    void mutateAllowsChangingTag() {
        SkillRef ref = SkillRef.parse("ghcr.io/org/skill:1.0.0");
        SkillRef retagged = ref.mutate().tag("2.0.0").build();

        assertThat(retagged.tag()).isEqualTo("2.0.0");
        assertThat(ref.tag()).isEqualTo("1.0.0");
        assertThat(retagged.registry()).isEqualTo(ref.registry());
        assertThat(retagged.repository()).isEqualTo(ref.repository());
    }

}
