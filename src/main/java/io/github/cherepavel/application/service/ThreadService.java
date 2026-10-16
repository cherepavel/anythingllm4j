package io.github.cherepavel.application.service;

import io.github.cherepavel.application.api.Threads;
import io.github.cherepavel.application.dto.ChatReply;
import io.github.cherepavel.application.dto.ChatRequest;
import io.github.cherepavel.application.dto.ThreadInfo;
import io.github.cherepavel.application.exception.AnythingLlmException;
import io.github.cherepavel.application.port.AnythingLlmGateway;
import java.util.Objects;

/** Validates thread commands and delegates external effects to the outbound port. */
public final class ThreadService implements Threads {
    private final AnythingLlmGateway gateway;
    public ThreadService(AnythingLlmGateway gateway) { this.gateway = Objects.requireNonNull(gateway, "gateway"); }

    @Override public ThreadInfo create(String workspaceSlug, String name) throws AnythingLlmException {
        return gateway.createThread(Arguments.nonblank(workspaceSlug), Arguments.nonblank(name));
    }
    @Override public ChatReply chat(String workspaceSlug, String threadSlug, ChatRequest request) throws AnythingLlmException {
        return gateway.chatThread(Arguments.nonblank(workspaceSlug), Arguments.nonblank(threadSlug),
                Objects.requireNonNull(request, "request"));
    }
}
