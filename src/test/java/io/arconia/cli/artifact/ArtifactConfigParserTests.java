package io.arconia.cli.artifact;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import land.oras.Config;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArtifactConfigParser}.
 */
class ArtifactConfigParserTests {

    private static final String MEDIA_TYPE = "application/vnd.test.config.v1+json";

    record TestConfig(String name, String version) {}

    @Test
    void fromObject() {
        TestConfig obj = new TestConfig("my-app", "1.0.0");
        Config config = ArtifactConfigParser.fromObject(obj, MEDIA_TYPE);

        assertThat(config.getMediaType()).isEqualTo(MEDIA_TYPE);
        assertThat(config.getDigest()).startsWith("sha256:");

        byte[] decoded = Base64.getDecoder().decode(config.getData());
        assertThat(config.getSize()).isEqualTo(decoded.length);

        String json = new String(decoded);
        assertThat(json).contains("my-app").contains("1.0.0");
    }

    @Test
    void fromObjectDataBytesMatchSerializedJson() {
        TestConfig obj = new TestConfig("my-app", "1.0.0");
        Config config = ArtifactConfigParser.fromObject(obj, MEDIA_TYPE);

        String decodedData = new String(config.getDataBytes());
        assertThat(decodedData).contains("\"name\"").contains("\"my-app\"");
    }

    @Test
    void fromObjectProducesDeterministicDigest() {
        TestConfig obj = new TestConfig("my-app", "1.0.0");
        Config first = ArtifactConfigParser.fromObject(obj, MEDIA_TYPE);
        Config second = ArtifactConfigParser.fromObject(obj, MEDIA_TYPE);

        assertThat(first.getDigest()).isEqualTo(second.getDigest());
    }

    @Test
    void fromObjectWithDifferentContentProducesDifferentDigest() {
        Config config1 = ArtifactConfigParser.fromObject(new TestConfig("app-a", "1.0.0"), MEDIA_TYPE);
        Config config2 = ArtifactConfigParser.fromObject(new TestConfig("app-b", "1.0.0"), MEDIA_TYPE);

        assertThat(config1.getDigest()).isNotEqualTo(config2.getDigest());
    }

}
