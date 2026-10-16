package io.github.cherepavel;

import com.google.gson.JsonParser;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnythingLlmClientTest {
    @Test
    void authenticatesAndPreservesApiPrefix() throws Exception {
        try (var server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("{\"authenticated\":true}"));
            var api = AnythingLlmClient.create(server.url("/proxy/api").toString(), "secret");
            assertTrue(api.authenticate().execute().body().getAsJsonObject().get("authenticated").getAsBoolean());
            var request = server.takeRequest(1, TimeUnit.SECONDS);
            assertEquals("/proxy/api/v1/auth", request.getPath());
            assertEquals("Bearer secret", request.getHeader("Authorization"));
        }
    }

    @Test
    void serializesChatAndReadsTypedResponse() throws Exception {
        try (var server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("{\"textResponse\":\"Привет\",\"close\":true,\"sources\":[]}"));
            var api = AnythingLlmClient.create(server.url("/api/").toString(), "secret");
            var response = api.chat("my workspace", new ChatRequest("Привет", ChatRequest.Mode.QUERY)).execute();
            assertEquals("Привет", response.body().textResponse());
            var request = server.takeRequest(1, TimeUnit.SECONDS);
            assertEquals("/api/v1/workspace/my%20workspace/chat", request.getPath());
            var json = JsonParser.parseString(request.getBody().readUtf8()).getAsJsonObject();
            assertEquals("query", json.get("mode").getAsString());
            assertFalse(json.has("sessionId"));
        }
    }

    @Test
    void sendsDeleteBodyAndExposesHttpErrors() throws Exception {
        try (var server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(403).setBody("{\"message\":\"Invalid API Key\"}"));
            var api = AnythingLlmClient.create(server.url("/api/").toString(), "secret");
            var response = api.removeDocuments(JsonParser.parseString("{\"names\":[\"folder/doc.json\"]}")).execute();
            assertEquals(403, response.code());
            try (var error = response.errorBody()) { assertTrue(error.string().contains("Invalid API Key")); }
            var request = server.takeRequest(1, TimeUnit.SECONDS);
            assertEquals("DELETE", request.getMethod());
            assertTrue(request.getBody().readUtf8().contains("folder/doc.json"));
        }
    }

    @Test
    void uploadsMultipartAndEncodesQuery() throws Exception {
        try (var server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("{}"));
            server.enqueue(new MockResponse().setBody("{}"));
            var api = AnythingLlmClient.create(server.url("/api/").toString(), "secret");
            var file = MultipartBody.Part.createFormData("file", "test.txt",
                    RequestBody.create(MediaType.get("text/plain"), "document text"));
            api.uploadDocumentToFolder("my folder", file, null, null).execute();
            var upload = server.takeRequest(1, TimeUnit.SECONDS);
            assertEquals("/api/v1/document/upload/my%20folder", upload.getPath());
            assertTrue(upload.getHeader("Content-Type").startsWith("multipart/form-data"));
            assertTrue(upload.getBody().readUtf8().contains("document text"));
            api.documents(Map.of("folder", "my folder", "limit", "all")).execute();
            assertTrue(server.takeRequest(1, TimeUnit.SECONDS).getPath().contains("folder=my%20folder"));
        }
    }

    @Test
    void streamsRawSseAndValidatesAllDeclarations() throws Exception {
        try (var server = new MockWebServer()) {
            server.enqueue(new MockResponse().setHeader("Content-Type", "text/event-stream")
                    .setBody("data: {\"textResponse\":\"hello\"}\n\ndata: [DONE]\n\n"));
            var api = AnythingLlmClient.create(server.url("/api/").toString(), "secret");
            try (var body = api.streamChat("demo", new ChatRequest("hi", ChatRequest.Mode.CHAT)).execute().body()) {
                assertEquals("data: {\"textResponse\":\"hello\"}", body.source().readUtf8Line());
            }
            // Eager validation catches invalid annotations on endpoints not otherwise exercised here.
            new retrofit2.Retrofit.Builder().baseUrl(server.url("/api/"))
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .validateEagerly(true).build().create(AnythingLlmApi.class);
        }
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> AnythingLlmClient.create("http://localhost/api/", " "));
        assertThrows(IllegalArgumentException.class, () -> AnythingLlmClient.create("http://localhost/api/?key=x", "key"));
    }
}
