package io.github.cherepavel;

import io.github.cherepavel.infrastructure.retrofit.LowLevelClientFactory;
import okhttp3.OkHttpClient;

/** Compatibility entry point for the low-level Retrofit API. */
public final class AnythingLlmClient {
    private AnythingLlmClient() { }

    /** Creates the legacy API using an instance URL including /api/. */
    public static AnythingLlmApi create(String apiUrl, String apiKey) {
        return LowLevelClientFactory.create(apiUrl, apiKey);
    }

    /** Caller retains ownership of the supplied transport and its shared resources. */
    public static AnythingLlmApi create(String apiUrl, String apiKey, OkHttpClient transport) {
        return LowLevelClientFactory.create(apiUrl, apiKey, transport);
    }
}
