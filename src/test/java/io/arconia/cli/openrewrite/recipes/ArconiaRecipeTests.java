package io.arconia.cli.openrewrite.recipes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ArconiaRecipe}.
 */
class ArconiaRecipeTests {

    @Test
    void computeRecipeLibraryThrowsWhenVersionIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ArconiaRecipe.computeRecipeLibrary(null));
    }

    @Test
    void computeRecipeLibraryThrowsWhenVersionIsEmpty() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ArconiaRecipe.computeRecipeLibrary(""));
    }

    @Test
    void computeRecipeLibraryThrowsWhenVersionIsCompletelyInvalid() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ArconiaRecipe.computeRecipeLibrary("pineapple"))
                .withMessageContaining("pineapple");
    }

    @Test
    void computeRecipeLibraryThrowsWhenVersionHasNoMinor() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ArconiaRecipe.computeRecipeLibrary("21"))
                .withMessageContaining("21");
    }

    @Test
    void computeRecipeLibraryWithMajorMinorVersion() {
        assertThat(ArconiaRecipe.computeRecipeLibrary("0.21"))
                .isEqualTo("io.arconia.rewrite.UpgradeArconia_0_21");
    }

    @Test
    void computeRecipeLibraryWithPatchVersion() {
        assertThat(ArconiaRecipe.computeRecipeLibrary("0.21.3"))
                .isEqualTo("io.arconia.rewrite.UpgradeArconia_0_21");
    }

    @Test
    void computeRecipeLibraryWithSnapshotVersion() {
        assertThat(ArconiaRecipe.computeRecipeLibrary("0.21.3-SNAPSHOT"))
                .isEqualTo("io.arconia.rewrite.UpgradeArconia_0_21");
    }

}
