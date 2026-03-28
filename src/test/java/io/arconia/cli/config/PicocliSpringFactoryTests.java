package io.arconia.cli.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link PicocliSpringFactory}.
 */
class PicocliSpringFactoryTests {

    @Test
    void whenApplicationContextIsNullThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new PicocliSpringFactory(null))
            .withMessage("applicationContext cannot be null");
    }

    @Test
    void createReturnsSpringBeanWhenAvailable() throws Exception {
        var context = new StaticApplicationContext();
        context.registerSingleton("myBean", MySpringBean.class);
        context.refresh();

        var factory = new PicocliSpringFactory(context);
        var result = factory.create(MySpringBean.class);

        assertThat(result).isInstanceOf(MySpringBean.class);
        assertThat(result).isEqualTo(context.getBean("myBean"));
        context.close();
    }

    @Test
    void createFallsBackToDefaultFactoryWhenNotASpringBean() throws Exception {
        var context = new StaticApplicationContext();
        context.refresh();

        var factory = new PicocliSpringFactory(context);
        var result = factory.create(MyPlainClass.class);

        assertThat(result).isInstanceOf(MyPlainClass.class);
        context.close();
    }

    static class MySpringBean {
    }

    static class MyPlainClass {
    }

}
