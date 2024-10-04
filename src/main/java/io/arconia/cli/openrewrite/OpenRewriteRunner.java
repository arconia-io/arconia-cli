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

    public void update(UpdateOptions updateOptions) {
        Assert.notNull(updateOptions, "updateOptions cannot be null");
        buildToolRunner.rewrite(updateOptions);
    }
  
}
