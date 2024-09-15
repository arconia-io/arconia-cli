package io.arconia.cli.openrewrite;

public final class OpenRewriteUtils {

    public static String getSpringBootUpdateRecipe(UpdateOptions updateOptions) {
        return switch (updateOptions.springBootVersion().trim()) {
            case "3.3" -> "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_3";
            case "3.2" -> "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_2";
            case "3.1" -> "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_1";
            case "3.0" -> "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_0";
            default -> "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_3";
        };
    }

}
