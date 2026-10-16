package io.github.cherepavel.application.exception;

/** More than one workspace matched the exact name. Messages contain only SDK operation names, never response payloads. */
public final class AmbiguousWorkspaceException extends AnythingLlmException {
    public AmbiguousWorkspaceException(String message) { super(message); }
    public AmbiguousWorkspaceException(String message, Throwable cause) { super(message, cause); }
}
