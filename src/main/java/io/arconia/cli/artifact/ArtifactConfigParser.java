package io.arconia.cli.artifact;

import java.util.Base64;

import land.oras.Config;
import land.oras.utils.SupportedAlgorithm;

import io.arconia.cli.json.JsonParser;

/**
 * Utility class for creating OCI Config objects from arbitrary objects.
 */
public final class ArtifactConfigParser {

    private ArtifactConfigParser() {}

    /**
     * Converts the given object to an OCI Config object with the given media type.
     */
    public static Config fromObject(Object configObject, String mediaType) {
        byte[] configBytes = JsonParser.toBytes(configObject);
        String digest = SupportedAlgorithm.SHA256.digest(configBytes);
        long size = configBytes.length;
        String dataBase64 = Base64.getEncoder().encodeToString(configBytes);

        String configDescriptor = """
            {
                "mediaType":"%s",
                "digest":"%s",
                "size":%d,
                "data":"%s"
            }""".formatted(mediaType, digest, size, dataBase64);

        return Config.fromJson(configDescriptor);
    }

}
