package io.github.cherepavel.application.exception;

/** No workspace matched the exact name or requested slug. Messages contain only SDK operation names, never response payloads. */
public final class WorkspaceNotFoundException extends AnythingLlmException {
    public WorkspaceNotFoundException(String message) { super(message); }
    public WorkspaceNotFoundException(String message, Throwable cause) { super(message, cause); }
}
