package io.github.cherepavel.application.dto;

import java.util.Map;

/** Typed server data. Fields retain an immutable snapshot of all original server properties. */
public record ChatSource(String id, String title, String text, String chunkSource, String documentId, String url, Map<String, Object> fields) {
    public ChatSource { fields = ServerFields.copyOf(fields); }
}
