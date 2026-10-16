package io.github.cherepavel.infrastructure.retrofit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.cherepavel.application.dto.ChatRequest;
import java.util.Locale;

/** Maps application commands to the versioned server payload. */
final class RequestMapper {
    private RequestMapper() { }

    static JsonObject chat(ChatRequest request) {
        JsonObject body = new JsonObject();
        body.addProperty("message", request.message());
        body.addProperty("mode", request.mode().name().toLowerCase(Locale.ROOT));
        if (request.sessionId() != null) body.addProperty("sessionId", request.sessionId());
        if (request.userId() != null) body.addProperty("userId", request.userId());
        if (request.reset() != null) body.addProperty("reset", request.reset());
        if (request.attachments() != null) {
            JsonArray attachments = new JsonArray();
            for (ChatRequest.Attachment entry : request.attachments()) {
                JsonObject attachment = new JsonObject();
                if (entry.name() != null) attachment.addProperty("name", entry.name());
                if (entry.mime() != null) attachment.addProperty("mime", entry.mime());
                if (entry.contentString() != null) attachment.addProperty("contentString", entry.contentString());
                attachments.add(attachment);
            }
            body.add("attachments", attachments);
        }
        return body;
    }
}
