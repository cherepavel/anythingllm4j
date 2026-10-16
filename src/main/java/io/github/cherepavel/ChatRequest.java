package io.github.cherepavel;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Objects;

/** Chat payload. Null optional fields are omitted from JSON. */
public record ChatRequest(String message, Mode mode, String sessionId, Long userId,
                          List<Attachment> attachments, Boolean reset) {
    public ChatRequest {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(mode, "mode");
        attachments = attachments == null ? null : List.copyOf(attachments);
    }

    /** Creates a chat without session, user, attachments, or history reset. */
    public ChatRequest(String message, Mode mode) {
        this(message, mode, null, null, null, null);
    }

    /** Conversation strategy; automatic mode is documented for workspace chats. */
    public enum Mode {
        @SerializedName("automatic") AUTOMATIC,
        @SerializedName("query") QUERY,
        @SerializedName("chat") CHAT
    }

    /** Attachment data URL. Documents require mime application/anythingllm-document. */
    public record Attachment(String name, String mime, String contentString) { }
}
