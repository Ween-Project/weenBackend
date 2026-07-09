package com.ween.dto.response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StringOrListDeserializer extends JsonDeserializer<List<String>> {
    @Override
    public List<String> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonToken currentToken = jp.getCurrentToken();
        
        if (currentToken == JsonToken.START_ARRAY) {
            return jp.readValueAs(new TypeReference<List<String>>() {});
        } else if (currentToken == JsonToken.VALUE_STRING) {
            String val = jp.getValueAsString();
            if (val == null || val.isBlank()) {
                return Collections.emptyList();
            }
            String[] lines = val.split("\n");
            List<String> result = new ArrayList<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        }
        
        return Collections.emptyList();
    }
}
