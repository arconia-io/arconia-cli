package io.arconia.cli.openrewrite.recipes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link SpringBootRecipe}.
 */
class SpringBootRecipeTests {

    @Test
    void computeRecipeLibraryThrowsWhenVersionIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SpringBootRecipe.computeRecipeLibrary(null));
    }

    @Test
    void computeRecipeLibraryThrowsWhenVersionIsEmpty() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SpringBootRecipe.computeRecipeLibrary(""));
    }

    @Test
    void computeRecipeLibraryThrowsWhenVersionIsCompletelyInvalid() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SpringBootRecipe.computeRecipeLibrary("pineapple"))
                .withMessageContaining("pineapple");
    }

    @Test
    void computeRecipeLibraryThrowsWhenVersionIsUnknown() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SpringBootRecipe.computeRecipeLibrary("21"))
                .withMessageContaining("21");
    }

    @Test
    void computeRecipeLibraryWithMajorMinorVersion() {
        assertThat(SpringBootRecipe.computeRecipeLibrary("3.5"))
                .isEqualTo("io.arconia.rewrite.spring.boot3.UpgradeSpringBoot_3_5");
    }

    @Test
    void computeRecipeLibraryWithPatchVersion() {
        assertThat(SpringBootRecipe.computeRecipeLibrary("3.5.3"))
                .isEqualTo("io.arconia.rewrite.spring.boot3.UpgradeSpringBoot_3_5");
    }

    @Test
    void computeRecipeLibraryWithSnapshotVersion() {
        assertThat(SpringBootRecipe.computeRecipeLibrary("3.5.3-SNAPSHOT"))
                .isEqualTo("io.arconia.rewrite.spring.boot3.UpgradeSpringBoot_3_5");
    }

}
