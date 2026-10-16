package io.github.cherepavel.application.dto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Creates deeply immutable snapshots of server values using only JDK types. */
final class ServerFields {
    private ServerFields() { }

    static Map<String, Object> copyOf(Map<String, ?> fields) {
        Objects.requireNonNull(fields, "fields");
        Map<String, Object> copy = new LinkedHashMap<>();
        fields.forEach((key, value) -> copy.put(Objects.requireNonNull(key, "field name"), snapshot(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object snapshot(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof BigDecimal || value instanceof BigInteger
                || value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof Float || value instanceof Double) return value;
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, entry) -> {
                if (!(key instanceof String name)) throw new IllegalArgumentException("field name must be a string");
                copy.put(name, snapshot(entry));
            });
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object entry : list) copy.add(snapshot(entry));
            return Collections.unmodifiableList(copy);
        }
        throw new IllegalArgumentException("unsupported server field value");
    }
}
