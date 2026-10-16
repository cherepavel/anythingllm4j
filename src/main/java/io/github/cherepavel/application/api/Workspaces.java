package io.github.cherepavel.application.api;

import io.github.cherepavel.application.dto.ChatReply;
import io.github.cherepavel.application.dto.ChatRequest;
import io.github.cherepavel.application.dto.Workspace;
import io.github.cherepavel.application.exception.AnythingLlmException;
import java.util.List;

/** Synchronous workspace use cases. HTTP handling is internal to the SDK. */
public interface Workspaces {
    List<Workspace> list() throws AnythingLlmException;
    Workspace get(String slug) throws AnythingLlmException;
    /** Requires exactly one case-sensitive match; absence and ambiguity are distinct checked failures. */
    Workspace findByName(String name) throws AnythingLlmException;
    ChatReply chat(String slug, ChatRequest request) throws AnythingLlmException;
}
