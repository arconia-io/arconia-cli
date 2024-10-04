package io.arconia.cli.openrewrite.recipes;

import org.springframework.util.Assert;

public enum SpringBootRecipe {
    SPRING_BOOT_3_3("3.3", "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_3"),
    SPRING_BOOT_3_2("3.2", "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_2"),
    SPRING_BOOT_3_1("3.1", "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_1"),
    SPRING_BOOT_3_0("3.0", "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_0");

    public static final String RECIPE_LIBRARY = "org.openrewrite.recipe:rewrite-spring";

    private final String version;
    private final String recipe;

    SpringBootRecipe(String version, String recipe) {
        this.version = version;
        this.recipe = recipe;
    }

    public String getVersion() {
        return version;
    }

    public String getRecipe() {
        return recipe;
    }

    public static String getRecipeName(String version) {
        Assert.hasText(version, "version cannot be null or empty");
        for (SpringBootRecipe springBootVersion : SpringBootRecipe.values()) {
            if (springBootVersion.getVersion().equals(version.trim())) {
                return springBootVersion.getRecipe();
            }
        }
        throw new IllegalArgumentException("Unsupported Spring Boot version: " + version);
    }
    
}
