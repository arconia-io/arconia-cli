package io.arconia.cli.openrewrite.recipes;

import org.springframework.util.Assert;

public enum SpringBootRecipe {
    SPRING_BOOT_4_0("4.0", "io.arconia.rewrite.spring.boot3.UpgradeSpringBoot_4_0"),
    SPRING_BOOT_3_5("3.5", "io.arconia.rewrite.spring.boot3.UpgradeSpringBoot_3_5");

    public static final String RECIPE_LIBRARY = "io.arconia.migrations:rewrite-spring";

    private final String version;
    private final String recipeLibrary;

    SpringBootRecipe(String version, String recipeLibrary) {
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
        for (SpringBootRecipe springBootVersion : SpringBootRecipe.values()) {
            if (version.trim().startsWith(springBootVersion.getVersion())) {
                return springBootVersion.getRecipeLibrary();
            }
        }
        throw new IllegalArgumentException("Unsupported Spring Boot version: " + version);
    }
    
}
