package io.arconia.cli.openrewrite.recipes;

import org.springframework.util.Assert;

public enum ArconiaRecipe {
    ARCONIA_0_13("0.13", "io.arconia.rewrite.UpgradeArconia_0_13"),
    ARCONIA_0_12("0.12", "io.arconia.rewrite.UpgradeArconia_0_12"),
    ARCONIA_0_11("0.11", "io.arconia.rewrite.UpgradeArconia_0_11"),
    ARCONIA_0_10("0.10", "io.arconia.rewrite.UpgradeArconia_0_10");

    public static final String RECIPE_LIBRARY = "io.arconia.migrations:rewrite-arconia";

    private final String version;
    private final String recipeLibrary;

    ArconiaRecipe(String version, String recipeLibrary) {
        this.version = version;
        this.recipeLibrary = recipeLibrary;
    }

    public String getVersion() {
        return version;
    }

    public String getRecipeLibrary() {
        return recipeLibrary;
    }

    public static String computeRecipeLibrary(String version) {
        Assert.hasText(version, "version cannot be null or empty");
        for (ArconiaRecipe arconiaVersion : ArconiaRecipe.values()) {
            if (version.trim().startsWith(arconiaVersion.getVersion())) {
                return arconiaVersion.getRecipeLibrary();
            }
        }
        throw new IllegalArgumentException("Unsupported Arconia version: " + version);
    }
    
}
