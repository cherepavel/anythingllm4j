package io.github.cherepavel.application.service;

import io.github.cherepavel.application.dto.*;
import io.github.cherepavel.application.exception.*;
import io.github.cherepavel.application.port.AnythingLlmGateway;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationServicesTest {
    @Test void exactSelectionIsIndependentOfTransport() throws Exception {
        var gateway = new StubGateway();
        var workspaces = new WorkspaceService(gateway);
        Workspace selected = workspace("water", "Water");
        gateway.workspaces = List.of(workspace("other", "water"), selected);
        assertSame(selected, workspaces.findByName("Water"));
        assertThrows(WorkspaceNotFoundException.class, () -> workspaces.findByName("WATER"));
        gateway.workspaces = List.of(selected, workspace("duplicate", "Water"));
        assertThrows(AmbiguousWorkspaceException.class, () -> workspaces.findByName("Water"));
    }

    @Test void invalidCommandsNeverReachTheGateway() {
        var gateway = new StubGateway();
        var threads = new ThreadService(gateway);
        var workspaces = new WorkspaceService(gateway);
        assertThrows(IllegalArgumentException.class, () -> threads.create("", "name"));
        assertThrows(IllegalArgumentException.class, () -> threads.create("water", " "));
        assertThrows(IllegalArgumentException.class, () -> threads.chat("water", " ", request()));
        assertThrows(NullPointerException.class, () -> threads.chat("water", "thread", null));
        assertThrows(IllegalArgumentException.class, () -> workspaces.get(" "));
        assertThrows(NullPointerException.class, () -> workspaces.chat("water", null));
        assertEquals(0, gateway.calls);
    }

    @Test void commandsAreDelegatedOnceAndResultsReturned() throws Exception {
        var gateway = new StubGateway();
        var threads = new ThreadService(gateway);
        var workspaces = new WorkspaceService(gateway);
        assertSame(gateway.thread, threads.create("water", "Analysis"));
        assertEquals(List.of("water", "Analysis"), gateway.arguments);
        assertSame(gateway.reply, threads.chat("water", "thread", request()));
        assertEquals(List.of("water", "thread"), gateway.arguments);
        assertSame(gateway.reply, workspaces.chat("water", request()));
        assertEquals(List.of("water"), gateway.arguments);
        assertEquals(3, gateway.calls);
    }

    @Test void applicationPreservesFailureIdentityAndCauseWithoutRetry() {
        var gateway = new StubGateway();
        var cause = new IOException("connection lost");
        gateway.failure = new TransportException("list workspaces: transport failure", cause);
        var failure = assertThrows(TransportException.class, () -> new WorkspaceService(gateway).findByName("Water"));
        assertSame(gateway.failure, failure);
        assertSame(cause, failure.getCause());
        assertEquals(1, gateway.calls);
    }

    private static ChatRequest request() { return new ChatRequest("question", ChatRequest.Mode.QUERY); }
    private static Workspace workspace(String slug, String name) { return new Workspace(null, slug, name, Map.of()); }

    private static final class StubGateway implements AnythingLlmGateway {
        private List<Workspace> workspaces = List.of();
        private final ThreadInfo thread = new ThreadInfo("1", "thread", "Analysis", Map.of());
        private final ChatReply reply = new ChatReply("2", "textResponse", "answer", List.of(), Map.of());
        private List<String> arguments;
        private int calls;
        private AnythingLlmException failure;
        @Override public List<Workspace> listWorkspaces() throws AnythingLlmException {
            calls++;
            if (failure != null) throw failure;
            return workspaces;
        }
        @Override public Workspace getWorkspace(String slug) { throw new AssertionError("unexpected get"); }
        @Override public ThreadInfo createThread(String slug, String name) {
            calls++; arguments = List.of(slug, name); return thread;
        }
        @Override public ChatReply chatWorkspace(String slug, ChatRequest request) {
            calls++; arguments = List.of(slug); return reply;
        }
        @Override public ChatReply chatThread(String slug, String threadSlug, ChatRequest request) {
            calls++; arguments = List.of(slug, threadSlug); return reply;
        }
    }
}
