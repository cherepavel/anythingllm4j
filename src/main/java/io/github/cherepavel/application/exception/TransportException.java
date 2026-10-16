package io.github.cherepavel.application.exception;

/** Connection, timeout or response reading failure. Messages contain only SDK operation names, never response payloads. */
public final class TransportException extends AnythingLlmException {
    public TransportException(String message) { super(message); }
    public TransportException(String message, Throwable cause) { super(message, cause); }
}
