package io.arconia.cli.openrewrite.recipes;

import org.springframework.util.Assert;

public final class ArconiaRecipe {

    public static final String RECIPE_LIBRARY = "io.arconia.migrations:rewrite-arconia";

    private static final String RECIPE_PREFIX = "io.arconia.rewrite.UpgradeArconia_";

    private ArconiaRecipe() {}

    public static String computeRecipeLibrary(String version) {
        Assert.hasText(version, "version cannot be null or empty");
        String trimmed = version.trim();
        String[] parts = trimmed.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid Arconia version: " + version);
        }
        return RECIPE_PREFIX + parts[0] + "_" + parts[1];
    }

}
