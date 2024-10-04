package io.arconia.cli.openrewrite.recipes;

import org.springframework.util.Assert;

public enum JavaRecipe {
    JAVA_11("11", "org.openrewrite.java.migrate.Java8toJava11"),
    JAVA_17("17", "org.openrewrite.java.migrate.UpgradeToJava17"),
    JAVA_21("21", "org.openrewrite.java.migrate.UpgradeToJava21");

    public static final String RECIPE_LIBRARY = "org.openrewrite.recipe:rewrite-migrate-java";

    private final String version;
    private final String recipe;

    JavaRecipe(String version, String recipe) {
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
        for (JavaRecipe javaVersion : JavaRecipe.values()) {
            if (javaVersion.getVersion().equals(version.trim())) {
                return javaVersion.getRecipe();
            }
        }
        throw new IllegalArgumentException("Unsupported Java version: " + version);
    }
    
}
