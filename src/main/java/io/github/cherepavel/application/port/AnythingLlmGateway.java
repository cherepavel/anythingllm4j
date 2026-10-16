package io.github.cherepavel.application.port;

import io.github.cherepavel.application.dto.ChatReply;
import io.github.cherepavel.application.dto.ChatRequest;
import io.github.cherepavel.application.dto.ThreadInfo;
import io.github.cherepavel.application.dto.Workspace;
import io.github.cherepavel.application.exception.AnythingLlmException;
import java.util.List;

/** Outbound capability required by use cases, independent of HTTP and serialization.
 * Implementations return validated, detached results and execute commands without implicit retries.
 */
public interface AnythingLlmGateway {
    List<Workspace> listWorkspaces() throws AnythingLlmException;
    Workspace getWorkspace(String slug) throws AnythingLlmException;
    ThreadInfo createThread(String workspaceSlug, String name) throws AnythingLlmException;
    ChatReply chatWorkspace(String slug, ChatRequest request) throws AnythingLlmException;
    ChatReply chatThread(String workspaceSlug, String threadSlug, ChatRequest request) throws AnythingLlmException;
}
