package io.github.cherepavel;

import com.google.gson.JsonElement;
import java.util.List;

/** Chat reply or SSE chunk. Inspect type and error even when HTTP status is successful. */
public record ChatResponse(String id, String type, String textResponse,
                           List<JsonElement> sources, Boolean close, JsonElement error) {
    public ChatResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
