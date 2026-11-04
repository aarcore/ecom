package com.ait.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * Small JSON helper for reading the JSON columns (product images, spec matrix)
 * without leaking Jackson boilerplate across services.
 */
@Slf4j
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtils() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("failed to serialise json: {}", ex.getMessage());
            return null;
        }
    }

    public static <T> T parse(String json, TypeReference<T> type, T fallback) {
        if (json == null || json.trim().isEmpty()) {
            return fallback;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception ex) {
            log.warn("failed to parse json '{}': {}", abbreviate(json), ex.getMessage());
            return fallback;
        }
    }

    public static List<String> parseStringList(String json) {
        return parse(json, new TypeReference<List<String>>() {
        }, Collections.emptyList());
    }

    public static <T> List<T> parseList(String json, Class<T> elementType) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception ex) {
            log.warn("failed to parse json list: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private static String abbreviate(String json) {
        return json.length() <= 64 ? json : json.substring(0, 64) + "...";
    }
}
