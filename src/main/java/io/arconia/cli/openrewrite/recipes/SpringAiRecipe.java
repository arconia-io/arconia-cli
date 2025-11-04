package io.arconia.cli.openrewrite.recipes;

import org.springframework.util.Assert;

public enum SpringAiRecipe {
    SPRING_AI_1_1("1.1", "io.arconia.rewrite.spring.ai.UpgradeSpringAi_1_1"),
    SPRING_AI_1_0("1.0", "io.arconia.rewrite.spring.ai.UpgradeSpringAi_1_0");

    public static final String RECIPE_LIBRARY = "io.arconia.migrations:rewrite-spring";

    private final String version;
    private final String recipeLibrary;

    SpringAiRecipe(String version, String recipeLibrary) {
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
        for (SpringAiRecipe springAiVersion : SpringAiRecipe.values()) {
            if (version.trim().startsWith(springAiVersion.getVersion())) {
                return springAiVersion.getRecipeLibrary();
            }
        }
        throw new IllegalArgumentException("Unsupported Spring AI version: " + version);
    }
    
}
