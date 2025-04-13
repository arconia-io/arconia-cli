package io.arconia.cli.openrewrite;

import org.springframework.util.Assert;

import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.core.ArconiaCliTerminal;

public class OpenRewriteRunner {

    private final BuildToolRunner buildToolRunner;

    public OpenRewriteRunner(ArconiaCliTerminal terminal) {
        Assert.notNull(terminal, "terminal cannot be null");
        this.buildToolRunner = BuildToolRunner.create(terminal);
    }

    public void update(UpdateOptions updateOptions, RecipeProvider recipeProvider) {
        Assert.notNull(updateOptions, "updateOptions cannot be null");
        buildToolRunner.update(updateOptions, recipeProvider);
    }

    public void rewrite(RewriteOptions rewriteOptions) {
        Assert.notNull(rewriteOptions, "rewriteOptions cannot be null");
        buildToolRunner.rewrite(rewriteOptions);
    }
  
}
