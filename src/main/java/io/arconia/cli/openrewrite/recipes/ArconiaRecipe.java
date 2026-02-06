package io.arconia.cli.openrewrite.recipes;

import org.springframework.util.Assert;

public enum ArconiaRecipe {
    ARCONIA_0_22("0.22", "io.arconia.rewrite.UpgradeArconia_0_22"),
    ARCONIA_0_21("0.21", "io.arconia.rewrite.UpgradeArconia_0_21"),
    ARCONIA_0_20("0.20", "io.arconia.rewrite.UpgradeArconia_0_20"),
    ARCONIA_0_19("0.19", "io.arconia.rewrite.UpgradeArconia_0_19"),
    ARCONIA_0_18("0.18", "io.arconia.rewrite.UpgradeArconia_0_18"),
    ARCONIA_0_17("0.17", "io.arconia.rewrite.UpgradeArconia_0_17"),
    ARCONIA_0_16("0.16", "io.arconia.rewrite.UpgradeArconia_0_16"),
    ARCONIA_0_15("0.15", "io.arconia.rewrite.UpgradeArconia_0_15"),
    ARCONIA_0_14("0.14", "io.arconia.rewrite.UpgradeArconia_0_14"),
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
