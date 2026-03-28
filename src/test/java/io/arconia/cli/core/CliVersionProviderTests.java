package io.arconia.cli.core;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link CliVersionProvider}.
 */
class CliVersionProviderTests {

    @Test
    void whenBuildPropertiesIsNullThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new CliVersionProvider(null))
            .withMessage("buildProperties cannot be null");
    }

    @Test
    void getVersionReturnsBuildVersion() {
        var properties = new Properties();
        properties.setProperty("version", "1.2.3");
        var versionProvider = new CliVersionProvider(new BuildProperties(properties));

        String[] version = versionProvider.getVersion();

        assertThat(version).containsExactly("1.2.3");
    }

    @Test
    void getVersionReturnsSnapshotVersion() {
        var properties = new Properties();
        properties.setProperty("version", "0.5.0-SNAPSHOT");
        var versionProvider = new CliVersionProvider(new BuildProperties(properties));

        String[] version = versionProvider.getVersion();

        assertThat(version).containsExactly("0.5.0-SNAPSHOT");
    }

}
