package io.arconia.cli.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * PicoCLI {@link IFactory} that delegates instantiation to the Spring
 * {@link ApplicationContext}, falling back to PicoCLI's default factory for
 * classes that are not Spring-managed beans (e.g., type converters).
 */
@Component
public final class PicocliSpringFactory implements IFactory {

    private final ApplicationContext applicationContext;
    private final IFactory defaultFactory = CommandLine.defaultFactory();

    public PicocliSpringFactory(ApplicationContext applicationContext) {
        Assert.notNull(applicationContext, "applicationContext cannot be null");
        this.applicationContext = applicationContext;
    }

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        try {
            return applicationContext.getBean(cls);
        } catch (BeansException ex) {
            return defaultFactory.create(cls);
        }
    }

}
