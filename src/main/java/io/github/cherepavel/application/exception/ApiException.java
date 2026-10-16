package io.github.cherepavel.application.exception;

/** The server reported an API failure. Messages contain only SDK operation names, never response payloads. */
public final class ApiException extends AnythingLlmException {
    public ApiException(String message) { super(message); }
    public ApiException(String message, Throwable cause) { super(message, cause); }
}
