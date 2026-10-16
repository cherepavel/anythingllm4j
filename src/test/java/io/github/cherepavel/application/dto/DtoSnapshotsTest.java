package io.github.cherepavel.application.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DtoSnapshotsTest {
    @Test void serverFieldsAreDeeplyDetachedAndPreserveNullsAndNumbers() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("nullable", null);
        nested.put("score", new BigDecimal("0.123456789123456789"));
        List<Object> links = new ArrayList<>();
        links.add(nested);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("links", links);
        var source = new ChatSource("id", "title", "text", null, null, null, fields);
        nested.put("score", 0);
        links.clear();
        fields.clear();
        List<?> snapshot = (List<?>) source.fields().get("links");
        Map<?, ?> item = (Map<?, ?>) snapshot.get(0);
        assertTrue(item.containsKey("nullable"));
        assertNull(item.get("nullable"));
        assertEquals(new BigDecimal("0.123456789123456789"), item.get("score"));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertThrows(UnsupportedOperationException.class, item::clear);
        assertThrows(UnsupportedOperationException.class, () -> source.fields().clear());
    }

    @Test void requestAttachmentsAndReplySourcesAreSnapshots() {
        var attachments = new ArrayList<ChatRequest.Attachment>();
        attachments.add(new ChatRequest.Attachment("doc", "text/plain", "content"));
        var request = new ChatRequest("question", ChatRequest.Mode.QUERY, null, null, attachments, null);
        attachments.clear();
        assertEquals(1, request.attachments().size());
        assertThrows(UnsupportedOperationException.class, () -> request.attachments().clear());
        var sources = new ArrayList<ChatSource>();
        sources.add(new ChatSource("id", "title", "text", null, null, null, Map.of()));
        var reply = new ChatReply("id", "textResponse", "answer", sources, Map.of());
        sources.clear();
        assertEquals(1, reply.sources().size());
        assertThrows(UnsupportedOperationException.class, () -> reply.sources().clear());
    }
}
