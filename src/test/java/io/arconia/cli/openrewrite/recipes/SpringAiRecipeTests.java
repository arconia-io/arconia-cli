package io.arconia.cli.openrewrite.recipes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link SpringAiRecipe}.
 */
class SpringAiRecipeTests {

    @Test
    void computeRecipeLibraryThrowsWhenVersionIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SpringAiRecipe.computeRecipeLibrary(null));
    }

    @Test
    void computeRecipeLibraryThrowsWhenVersionIsEmpty() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SpringAiRecipe.computeRecipeLibrary(""));
    }

    @Test
    void computeRecipeLibraryThrowsWhenVersionIsCompletelyInvalid() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SpringAiRecipe.computeRecipeLibrary("pineapple"))
                .withMessageContaining("pineapple");
    }

    @Test
    void computeRecipeLibraryThrowsWhenVersionIsUnknown() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SpringAiRecipe.computeRecipeLibrary("21"))
                .withMessageContaining("21");
    }

    @Test
    void computeRecipeLibraryWithMajorMinorVersion() {
        assertThat(SpringAiRecipe.computeRecipeLibrary("1.0"))
                .isEqualTo("io.arconia.rewrite.spring.ai.UpgradeSpringAi_1_0");
    }

    @Test
    void computeRecipeLibraryWithPatchVersion() {
        assertThat(SpringAiRecipe.computeRecipeLibrary("1.0.3"))
                .isEqualTo("io.arconia.rewrite.spring.ai.UpgradeSpringAi_1_0");
    }

    @Test
    void computeRecipeLibraryWithSnapshotVersion() {
        assertThat(SpringAiRecipe.computeRecipeLibrary("1.0.3-SNAPSHOT"))
                .isEqualTo("io.arconia.rewrite.spring.ai.UpgradeSpringAi_1_0");
    }

}
