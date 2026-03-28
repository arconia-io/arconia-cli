package io.arconia.cli.openrewrite;

import java.util.List;

import org.springframework.util.Assert;

import io.arconia.cli.build.BuildToolRunner;
import io.arconia.cli.commands.options.OutputOptions;

public class OpenRewriteRunner {

    private final BuildToolRunner buildToolRunner;

    public OpenRewriteRunner(OutputOptions outputOptions, List<String> additionalParameters) {
        Assert.notNull(outputOptions, "outputOptions cannot be null");
        Assert.notNull(additionalParameters, "additionalParameters cannot be null");
        this.buildToolRunner = BuildToolRunner.create(outputOptions, additionalParameters);
    }

    public void update(UpdateArguments updateArguments) {
        Assert.notNull(updateArguments, "updateArguments cannot be null");
        buildToolRunner.update(updateArguments);
    }

    public void rewrite(RewriteArguments rewriteArguments) {
        Assert.notNull(rewriteArguments, "rewriteArguments cannot be null");
        buildToolRunner.rewrite(rewriteArguments);
    }

}
