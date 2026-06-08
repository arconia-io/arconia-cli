package io.arconia.cli.openrewrite.recipes;

import org.springframework.util.Assert;

public final class SpringBootRecipe {

    public static final String RECIPE_LIBRARY = "io.arconia.migrations:rewrite-spring";

    private SpringBootRecipe() {}

    public static String computeRecipeLibrary(String version) {
        Assert.hasText(version, "version cannot be null or empty");
        String trimmed = version.trim();
        String[] parts = trimmed.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid Spring Boot version: " + version);
        }
        return "io.arconia.rewrite.spring.boot" + parts[0] + ".UpgradeSpringBoot_" + parts[0] + "_" + parts[1];
    }

}
