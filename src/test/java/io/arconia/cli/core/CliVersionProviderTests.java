package io.arconia.cli.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CliVersionProvider}.
 */
@SpringBootTest
class CliVersionProviderTests {

    @Test
    void getVersionReturnsBuildVersion() throws Exception {
        var versionProvider = new CliVersionProvider();

        String[] version = versionProvider.getVersion();

        assertThat(version[0]).contains("0.");
    }

}
