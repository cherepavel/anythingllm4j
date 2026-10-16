package io.github.cherepavel.application.dto;

import java.util.Map;

/** Typed server data. Fields retain an immutable snapshot of all original server properties. */
public record ThreadInfo(String id, String slug, String name, Map<String, Object> fields) {
    public ThreadInfo { fields = ServerFields.copyOf(fields); }
}
