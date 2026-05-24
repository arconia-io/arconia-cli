package io.arconia.cli.core;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import picocli.CommandLine.IVersionProvider;

public final class CliVersionProvider implements IVersionProvider {

    private final BuildProperties buildProperties;

    public CliVersionProvider() throws IOException {
        this.buildProperties = new BuildProperties(loadProperties());
    }

    @Override
    public String[] getVersion() {
        return new String[] { buildProperties.getVersion() };
    }

    private Properties loadProperties() throws IOException {
        Resource resource = new ClassPathResource("META-INF/build-info.properties");
        Properties source = PropertiesLoaderUtils.loadProperties(new EncodedResource(resource, Charset.defaultCharset()));
        Properties target = new Properties();
        String prefix = "build.";
        for (String key : source.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                target.put(key.substring(prefix.length()), source.get(key));
            }
        }
        return target;
    }

}
