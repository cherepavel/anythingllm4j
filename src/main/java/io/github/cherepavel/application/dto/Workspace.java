package io.github.cherepavel.application.dto;

import java.util.Map;

/** Typed server data. Fields retain an immutable snapshot of all original server properties. */
public record Workspace(String id, String slug, String name, Map<String, Object> fields) {
    public Workspace { fields = ServerFields.copyOf(fields); }
}
