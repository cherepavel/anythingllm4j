package io.github.cherepavel.application.exception;

/** Unsuccessful HTTP response. The response payload is deliberately excluded. */
public final class HttpException extends AnythingLlmException {
    private final int statusCode;
    private final String operation;
    public HttpException(int statusCode, String operation) {
        super(operation + ": HTTP " + statusCode);
        this.statusCode = statusCode;
        this.operation = operation;
    }
    public int statusCode() { return statusCode; }
    public String operation() { return operation; }
}
