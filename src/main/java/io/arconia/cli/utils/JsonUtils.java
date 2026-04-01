package io.arconia.cli.utils;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

public final class JsonUtils {

    public static JsonMapper getJsonMapper() {
        return JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                .build();
    }

}
