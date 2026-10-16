package io.github.cherepavel;

import java.time.Duration;
import java.util.Objects;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Factory for an authenticated Retrofit service. Reuse the returned service across calls. */
public final class AnythingLlmClient {
    private AnythingLlmClient() { }

    /**
     * Creates a service for an instance API URL, for example http://localhost:3001/api/.
     * Calls support synchronous execute(), asynchronous enqueue(), and cancellation.
     */
    public static AnythingLlmApi create(String apiUrl, String apiKey) {
        return create(apiUrl, apiKey, new OkHttpClient.Builder()
                .readTimeout(Duration.ofMinutes(5)).build());
    }

    /** Creates a service with caller-provided transport settings and connection pool. */
    public static AnythingLlmApi create(String apiUrl, String apiKey, OkHttpClient transport) {
        Objects.requireNonNull(apiUrl, "apiUrl");
        Objects.requireNonNull(apiKey, "apiKey");
        Objects.requireNonNull(transport, "transport");
        if (apiKey.isBlank() || apiKey.indexOf('\r') >= 0 || apiKey.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("apiKey must be nonblank and contain no line breaks");
        }
        HttpUrl url = HttpUrl.get(apiUrl.endsWith("/") ? apiUrl : apiUrl + "/");
        if (url.query() != null || url.fragment() != null
                || !url.username().isEmpty() || !url.password().isEmpty()) {
            throw new IllegalArgumentException("apiUrl must not contain credentials, query, or fragment");
        }
        OkHttpClient client = transport.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        .header("Authorization", "Bearer " + apiKey).build()))
                .build();
        return new Retrofit.Builder().baseUrl(url).client(client)
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(AnythingLlmApi.class);
    }
}
