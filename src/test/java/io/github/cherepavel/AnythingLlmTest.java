package io.github.cherepavel;

import io.github.cherepavel.application.dto.*;
import io.github.cherepavel.application.dto.ChatRequest;
import io.github.cherepavel.application.exception.*;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnythingLlmTest {
    private static final String KEY = "super-secret-key";
    private static final ChatRequest QUESTION = new ChatRequest("private question", ChatRequest.Mode.QUERY);

    private AnythingLlm client(MockWebServer server) {
        return AnythingLlm.builder().serverUri(server.url("/").uri()).apiKey(KEY).build();
    }

    @Test void typedWorkflowPreservesSourcesAndAuthorization() throws Exception {
        try (var server = new MockWebServer(); var client = client(server)) {
            server.enqueue(new MockResponse().setBody("{\"workspaces\":[{\"id\":12,\"slug\":\"water\",\"name\":\"СанПиН — вода\",\"extra\":42}]}"));
            server.enqueue(new MockResponse().setBody("{\"thread\":{\"id\":3,\"slug\":\"analysis\",\"name\":\"Проверка анализа\"}}"));
            server.enqueue(new MockResponse().setBody("""
                    {"type":"textResponse","textResponse":"Норматив","sources":[
                    {"id":"chunk","title":"СанПиН","text":"фрагмент","docId":4,
                     "chunkSource":"https://example.org/doc","url":"https://example.org/page",
                     "metadata":{"custom":true}}]}
                    """));
            Workspace workspace = client.workspaces().findByName("СанПиН — вода");
            assertEquals("12", workspace.id());
            var copy = workspace.fields(); assertThrows(UnsupportedOperationException.class, () -> copy.remove("extra"));
            assertTrue(workspace.fields().containsKey("extra"));
            ThreadInfo thread = client.threads().create(workspace.slug(), "Проверка анализа");
            ChatReply reply = client.threads().chat(workspace.slug(), thread.slug(), QUESTION);
            assertEquals("Норматив", reply.textResponse());
            ChatSource source = reply.sources().get(0);
            assertEquals("СанПиН", source.title());
            assertEquals("фрагмент", source.text());
            assertEquals("chunk", source.id());
            assertEquals("4", source.documentId());
            assertEquals("https://example.org/doc", source.chunkSource());
            assertEquals("https://example.org/page", source.url());
            assertEquals(java.util.Map.of("custom", true), source.fields().get("metadata"));
            for (String path : new String[]{"/api/v1/workspaces", "/api/v1/workspace/water/thread/new",
                    "/api/v1/workspace/water/thread/analysis/chat"}) {
                var request = server.takeRequest(1, TimeUnit.SECONDS);
                assertEquals(path, request.getPath());
                assertEquals("Bearer " + KEY, request.getHeader("Authorization"));
            }
        }
    }

    @Test void slugLookupAndWorkspaceChatWithAbsentSources() throws Exception {
        try (var server = new MockWebServer(); var client = client(server)) {
            server.enqueue(new MockResponse().setBody("{\"workspace\":[{\"name\":\"Water\",\"slug\":\"water\"}]}"));
            server.enqueue(new MockResponse().setBody("{\"textResponse\":\"ok\"}"));
            assertEquals("water", client.workspaces().get("water").slug());
            assertTrue(client.workspaces().chat("water", QUESTION).sources().isEmpty());
        }
    }

    @Test void mapsHttpAndApiFailuresWithoutEchoingServerPayloads() throws Exception {
        try (var server = new MockWebServer(); var client = client(server)) {
            for (int code : new int[]{401, 403, 429, 500, 503, 307}) {
                server.enqueue(new MockResponse().setResponseCode(code).setHeader("Retry-After", "0")
                        .setBody(KEY + " private question"));
                HttpException error = assertThrows(HttpException.class, () -> client.workspaces().chat("water", QUESTION));
                assertEquals(code, error.statusCode());
                assertEquals("workspace chat", error.operation());
                assertSafe(error);
            }
            for (String body : new String[]{"{\"type\":\"abort\"}",
                    "{\"error\":\"" + KEY + " private question\"}",
                    "{\"success\":false}", "{\"error\":{\"message\":\"failure\"}}"}) {
                server.enqueue(new MockResponse().setBody(body));
                assertSafe(assertThrows(ApiException.class, () -> client.workspaces().chat("water", QUESTION)));
            }
            assertEquals(10, server.getRequestCount());
        }
    }

    @Test void rejectsMalformedEmptyAndIncompleteResponses() throws Exception {
        try (var server = new MockWebServer(); var client = client(server)) {
            for (String body : new String[]{"", "{", "not json", "null", "[]", "{}", "{\"" + KEY + "\": [}",
                    "{\"textResponse\":null}", "{\"textResponse\":42}", "{\"textResponse\":\"ok\",\"sources\":{}}",
                    "{textResponse:'ok'}", "{\"textResponse\":\"ok\"} {}"}) {
                server.enqueue(new MockResponse().setBody(body));
                assertSafe(assertThrows(InvalidResponseException.class, () -> client.workspaces().chat("water", QUESTION)));
            }
            server.enqueue(new MockResponse().setResponseCode(204));
            assertThrows(InvalidResponseException.class, () -> client.workspaces().list());
        }
    }

    @Test void distinguishesMissingAndAmbiguousExactNames() throws Exception {
        try (var server = new MockWebServer(); var client = client(server)) {
            server.enqueue(new MockResponse().setBody("{\"workspaces\":[]}"));
            assertThrows(WorkspaceNotFoundException.class, () -> client.workspaces().findByName("Water"));
            server.enqueue(new MockResponse().setBody("{\"workspaces\":[{\"name\":\"Water\",\"slug\":\"a\"},{\"name\":\"Water\",\"slug\":\"b\"}]}"));
            assertThrows(AmbiguousWorkspaceException.class, () -> client.workspaces().findByName("Water"));
            server.enqueue(new MockResponse().setBody("{\"workspaces\":[{\"name\":\"water\",\"slug\":\"a\"}]}"));
            assertThrows(WorkspaceNotFoundException.class, () -> client.workspaces().findByName("Water"));
        }
    }

    @Test void totalTimeoutIncludesSlowResponseReading() throws Exception {
        try (var server = new MockWebServer(); var client = AnythingLlm.builder()
                .serverUri(server.url("/").uri()).apiKey(KEY).requestTimeout(Duration.ofMillis(500)).build()) {
            server.enqueue(new MockResponse().setBody("{\"workspaces\":[]}"));
            client.workspaces().list();
            server.enqueue(new MockResponse().setBody("{\"workspaces\":[]}").throttleBody(1, 100, TimeUnit.MILLISECONDS));
            long start = System.nanoTime();
            var error = assertThrows(TransportException.class, () -> client.workspaces().list());
            assertNotNull(error.getCause());
            assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(2)) < 0);
            assertSafe(error);
            assertNotNull(server.takeRequest(1, TimeUnit.SECONDS));
            assertNotNull(server.takeRequest(1, TimeUnit.SECONDS));
            assertEquals(2, server.getRequestCount());
        }
    }

    @Test void doesNotRetryPostAfterServerDisconnects() throws Exception {
        for (boolean create : new boolean[]{true, false}) {
            try (var server = new MockWebServer(); var client = client(server)) {
                server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
                assertThrows(TransportException.class, () -> {
                    if (create) client.threads().create("water", "new");
                    else client.threads().chat("water", "thread", QUESTION);
                });
                assertNotNull(server.takeRequest(1, TimeUnit.SECONDS));
                assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS));
                assertEquals(1, server.getRequestCount());
            }
        }
    }

    @Test void validatesConfigurationAndClosedState() throws Exception {
        for (String uri : new String[]{"ftp://localhost", "http://user:pass@localhost", "http://localhost?key=x",
                "http://localhost#fragment", "http://localhost/api/", "http://localhost/api/v1", "http://localhost:99999"}) {
            assertSafe(assertThrows(IllegalArgumentException.class, () -> AnythingLlm.builder()
                    .serverUri(URI.create(uri)).apiKey(KEY).build()));
        }
        for (String key : new String[]{"", " ", "Bearer " + KEY, KEY + "\n", "ключ"}) {
            assertSafe(assertThrows(IllegalArgumentException.class, () -> AnythingLlm.builder()
                    .serverUri(URI.create("http://localhost")).apiKey(key).build()));
        }
        for (Duration duration : new Duration[]{Duration.ZERO, Duration.ofNanos(1), Duration.ofSeconds(-1), Duration.ofDays(100)}) {
            assertThrows(IllegalArgumentException.class, () -> AnythingLlm.builder().serverUri(URI.create("http://localhost"))
                    .apiKey(KEY).requestTimeout(duration).build());
        }
        var builder = AnythingLlm.builder().apiKey(KEY);
        assertFalse(builder.toString().contains(KEY));
        try (var server = new MockWebServer()) {
            var client = client(server); client.close(); client.close();
            assertThrows(IllegalStateException.class, () -> client.workspaces().list());
            assertFalse(client.toString().contains(KEY));
        }
    }

    @Test void appendsApiAfterReverseProxyPrefix() throws Exception {
        try (var server = new MockWebServer(); var client = AnythingLlm.builder()
                .serverUri(server.url("/proxy/").uri()).apiKey(KEY).build()) {
            server.enqueue(new MockResponse().setBody("{\"workspaces\":[]}"));
            client.workspaces().list();
            assertEquals("/proxy/api/v1/workspaces", server.takeRequest().getPath());
        }
    }

    private static void assertSafe(Throwable error) {
        assertFalse(error.toString().contains(KEY));
        assertFalse(error.toString().contains("private question"));
        if (error.getCause() != null) assertSafe(error.getCause());
    }
}
