package io.arconia.cli.config;

import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import picocli.CommandLine.IVersionProvider;

@Component
public final class VersionProvider implements IVersionProvider {

    private final BuildProperties buildProperties;

    public VersionProvider(BuildProperties buildProperties) {
        Assert.notNull(buildProperties, "buildProperties cannot be null");
        this.buildProperties = buildProperties;
    }

    @Override
    public String[] getVersion() {
        return new String[] { buildProperties.getVersion() };
    }

}
