package io.github.cherepavel.infrastructure.retrofit;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.github.cherepavel.application.dto.ChatReply;
import io.github.cherepavel.application.dto.ChatSource;
import io.github.cherepavel.application.dto.Workspace;
import io.github.cherepavel.application.exception.AnythingLlmException;
import io.github.cherepavel.application.exception.ApiException;
import io.github.cherepavel.application.exception.InvalidResponseException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns the server's JSON envelopes and converts them to detached application DTOs. */
final class ResponseMapper {
    private static final Gson GSON = new Gson();
    private ResponseMapper() { }

    static JsonObject parse(String json, String operation) throws AnythingLlmException {
        final JsonObject root;
        try {
            // Server-controlled property names must not appear in parser diagnostics.
            JsonReader reader = new JsonReader(new StringReader(json)) {
                @Override public String getPath() { return "$"; }
                @Override public String getPreviousPath() { return "$"; }
            };
            reader.setStrictness(Strictness.STRICT);
            JsonElement value = GSON.getAdapter(JsonElement.class).read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) throw invalid(operation);
            root = object(value);
        } catch (IOException | RuntimeException e) {
            // Preserve the parser failure, with server-controlled JSON paths excluded above.
            throw new InvalidResponseException(operation + ": invalid JSON response", e);
        }
        JsonElement error = root.get("error");
        if (hasError(error) || "abort".equals(optional(root, "type"))
                || (root.has("success") && root.get("success").isJsonPrimitive()
                && root.get("success").getAsJsonPrimitive().isBoolean() && !root.get("success").getAsBoolean())) {
            throw new ApiException(operation + ": API reported failure");
        }
        return root;
    }

    private static boolean hasError(JsonElement value) {
        if (value == null || value.isJsonNull()) return false;
        if (value.isJsonPrimitive()) {
            var primitive = value.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isString()) return !primitive.getAsString().isBlank();
        }
        return true;
    }
    static Workspace workspace(JsonObject value) throws InvalidResponseException {
        return new Workspace(optional(value, "id"), required(value, "slug"), required(value, "name"), fields(value));
    }
    static ChatReply reply(JsonObject value) throws InvalidResponseException {
        String text = required(value, "textResponse");
        List<ChatSource> sources = new ArrayList<>();
        JsonElement entries = value.get("sources");
        if (entries != null && !entries.isJsonNull()) {
            if (!entries.isJsonArray()) throw invalid("chat sources");
            for (JsonElement entry : entries.getAsJsonArray()) {
                JsonObject source = object(entry);
                sources.add(new ChatSource(optional(source, "id"), optional(source, "title"),
                        optional(source, "text"), optional(source, "chunkSource"),
                        optional(source, "docId"), optional(source, "url"), fields(source)));
            }
        }
        return new ChatReply(optional(value, "id"), optional(value, "type"), text, sources, fields(value));
    }
    static JsonObject object(JsonElement value) throws InvalidResponseException {
        if (value == null || !value.isJsonObject()) throw invalid("decode response");
        return value.getAsJsonObject();
    }
    static String required(JsonObject value, String key) throws InvalidResponseException {
        String result = optional(value, key);
        if (result == null || result.isBlank() || !value.get(key).getAsJsonPrimitive().isString())
            throw invalid("decode response");
        return result;
    }
    static String optional(JsonObject value, String key) throws InvalidResponseException {
        JsonElement field = value.get(key);
        if (field == null || field.isJsonNull()) return null;
        if (!field.isJsonPrimitive() || field.getAsJsonPrimitive().isBoolean()) throw invalid("decode response");
        return field.getAsString();
    }
    static InvalidResponseException invalid(String operation) {
        return new InvalidResponseException(operation + ": missing or invalid response fields");
    }

    static Map<String, Object> fields(JsonObject object) {
        Map<String, Object> result = new LinkedHashMap<>();
        object.entrySet().forEach(entry -> result.put(entry.getKey(), field(entry.getValue())));
        return result;
    }

    private static Object field(JsonElement value) {
        if (value.isJsonNull()) return null;
        if (value.isJsonObject()) return fields(value.getAsJsonObject());
        if (value.isJsonArray()) {
            List<Object> result = new ArrayList<>();
            value.getAsJsonArray().forEach(entry -> result.add(field(entry)));
            return result;
        }
        var primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isNumber()) return new BigDecimal(primitive.getAsString());
        return primitive.getAsString();
    }
}
