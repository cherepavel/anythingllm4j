package io.github.cherepavel.application.dto;

import java.util.Map;
import java.util.List;

/** Validated synchronous chat result. Missing sources become an empty list. */
public record ChatReply(String id, String type, String textResponse,
                        List<ChatSource> sources, Map<String, Object> fields) {
    public ChatReply {
        sources = List.copyOf(sources);
        fields = ServerFields.copyOf(fields);
    }
}
