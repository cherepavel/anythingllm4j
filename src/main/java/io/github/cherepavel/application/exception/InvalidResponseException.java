package io.github.cherepavel.application.exception;

/** The server response violated the expected JSON contract. Messages contain only SDK operation names, never response payloads. */
public final class InvalidResponseException extends AnythingLlmException {
    public InvalidResponseException(String message) { super(message); }
    public InvalidResponseException(String message, Throwable cause) { super(message, cause); }
}
