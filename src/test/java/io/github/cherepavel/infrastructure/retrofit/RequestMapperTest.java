package io.github.cherepavel.infrastructure.retrofit;

import com.google.gson.JsonParser;
import io.github.cherepavel.application.dto.ChatRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestMapperTest {
    @Test void serializesAllOptionalFieldsAndAttachmentsToServerShape() {
        var request = new ChatRequest("question", ChatRequest.Mode.QUERY, "session", 42L,
                List.of(new ChatRequest.Attachment("document", "text/plain", "data:text/plain;base64,WA==")), true);
        var expected = JsonParser.parseString("""
                {"message":"question","mode":"query","sessionId":"session","userId":42,"reset":true,
                 "attachments":[{"name":"document","mime":"text/plain","contentString":"data:text/plain;base64,WA=="}]}
                """);
        assertEquals(expected, RequestMapper.chat(request));
    }

    @Test void omitsNullFieldsAndUsesWireNamesForEveryMode() {
        for (ChatRequest.Mode mode : ChatRequest.Mode.values()) {
            var json = RequestMapper.chat(new ChatRequest("question", mode));
            String expectedMode = switch (mode) {
                case AUTOMATIC -> "automatic";
                case QUERY -> "query";
                case CHAT -> "chat";
            };
            assertEquals(expectedMode, json.get("mode").getAsString());
            assertEquals(2, json.size());
        }
    }
}
