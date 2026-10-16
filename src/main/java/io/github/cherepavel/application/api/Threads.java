package io.github.cherepavel.application.api;

import io.github.cherepavel.application.dto.ChatReply;
import io.github.cherepavel.application.dto.ChatRequest;
import io.github.cherepavel.application.dto.ThreadInfo;
import io.github.cherepavel.application.exception.AnythingLlmException;

/** Synchronous thread commands; mutations must not be retried implicitly. */
public interface Threads {
    ThreadInfo create(String workspaceSlug, String name) throws AnythingLlmException;
    ChatReply chat(String workspaceSlug, String threadSlug, ChatRequest request) throws AnythingLlmException;
}
