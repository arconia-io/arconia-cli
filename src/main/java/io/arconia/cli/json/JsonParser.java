package io.arconia.cli.json;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * Utility class for parsing and serializing JSON.
 */
public final class JsonParser {

    private static final JsonMapper jsonMapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .build();

    private JsonParser() {}

    public static byte[] toBytes(Object object) {
        return jsonMapper.writeValueAsBytes(object);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return jsonMapper.readValue(json, type);
    }

    public static String toJson(Object object) {
        return jsonMapper.writeValueAsString(object);
    }

    public static String toJsonPrettyPrint(Object object) {
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

}
