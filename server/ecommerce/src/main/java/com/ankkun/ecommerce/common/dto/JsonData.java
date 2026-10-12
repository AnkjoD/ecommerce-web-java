package com.ankkun.ecommerce.common.dto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
public final class JsonData {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonData() {}
    public static String store(JsonNode node) { return node == null ? "{}" : node.toString(); }
    public static JsonNode parse(String value) {
        try { return MAPPER.readTree(value == null ? "{}" : value); }
        catch (Exception e) { throw new IllegalStateException("Invalid stored JSON", e); }
    }
}
