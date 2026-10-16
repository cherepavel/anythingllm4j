package io.github.cherepavel.application.service;

import io.github.cherepavel.application.api.Workspaces;
import io.github.cherepavel.application.dto.ChatReply;
import io.github.cherepavel.application.dto.ChatRequest;
import io.github.cherepavel.application.dto.Workspace;
import io.github.cherepavel.application.exception.AmbiguousWorkspaceException;
import io.github.cherepavel.application.exception.AnythingLlmException;
import io.github.cherepavel.application.exception.WorkspaceNotFoundException;
import io.github.cherepavel.application.port.AnythingLlmGateway;
import java.util.List;
import java.util.Objects;

/** Workspace selection rules and application input validation. */
public final class WorkspaceService implements Workspaces {
    private final AnythingLlmGateway gateway;
    public WorkspaceService(AnythingLlmGateway gateway) { this.gateway = Objects.requireNonNull(gateway, "gateway"); }

    @Override public List<Workspace> list() throws AnythingLlmException { return List.copyOf(gateway.listWorkspaces()); }
    @Override public Workspace get(String slug) throws AnythingLlmException {
        return gateway.getWorkspace(Arguments.nonblank(slug));
    }
    @Override public Workspace findByName(String name) throws AnythingLlmException {
        Objects.requireNonNull(name, "name");
        List<Workspace> matches = list().stream().filter(workspace -> name.equals(workspace.name())).toList();
        if (matches.isEmpty()) throw new WorkspaceNotFoundException("find workspace by name: no match");
        if (matches.size() > 1) throw new AmbiguousWorkspaceException("find workspace by name: multiple matches");
        return matches.get(0);
    }
    @Override public ChatReply chat(String slug, ChatRequest request) throws AnythingLlmException {
        return gateway.chatWorkspace(Arguments.nonblank(slug), Objects.requireNonNull(request, "request"));
    }
}
