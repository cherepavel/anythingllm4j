package io.github.cherepavel.application.exception;

/** Base checked failure of a synchronous SDK operation. Messages contain only SDK operation names, never response payloads. */
public class AnythingLlmException extends Exception {
    public AnythingLlmException(String message) { super(message); }
    public AnythingLlmException(String message, Throwable cause) { super(message, cause); }
}
