package io.github.cherepavel.application.service;

/** Shared application argument invariants; never includes caller values in diagnostics. */
final class Arguments {
    private Arguments() { }
    static String nonblank(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("argument must be nonblank");
        return value;
    }
}
