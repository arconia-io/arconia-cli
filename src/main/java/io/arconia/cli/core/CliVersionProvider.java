package io.arconia.cli.core;

import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import picocli.CommandLine.IVersionProvider;

@Component
public final class CliVersionProvider implements IVersionProvider {

    private final BuildProperties buildProperties;

    public CliVersionProvider(BuildProperties buildProperties) {
        Assert.notNull(buildProperties, "buildProperties cannot be null");
        this.buildProperties = buildProperties;
    }

    @Override
    public String[] getVersion() {
        return new String[] { buildProperties.getVersion() };
    }

}
