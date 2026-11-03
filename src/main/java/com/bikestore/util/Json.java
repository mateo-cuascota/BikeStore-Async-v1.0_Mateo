package com.bikestore.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Conversion Pedido <-> JSON con Jackson.
 */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Json() {
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo convertir a JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> tipo) {
        try {
            return MAPPER.readValue(json, tipo);
        } catch (Exception e) {
            throw new RuntimeException("JSON invalido: " + e.getMessage(), e);
        }
    }
}
