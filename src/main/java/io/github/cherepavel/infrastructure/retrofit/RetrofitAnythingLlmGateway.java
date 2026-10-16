package io.github.cherepavel.infrastructure.retrofit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.cherepavel.application.dto.ChatReply;
import io.github.cherepavel.application.dto.ChatRequest;
import io.github.cherepavel.application.dto.ThreadInfo;
import io.github.cherepavel.application.dto.Workspace;
import io.github.cherepavel.application.exception.AnythingLlmException;
import io.github.cherepavel.application.exception.HttpException;
import io.github.cherepavel.application.exception.TransportException;
import io.github.cherepavel.application.exception.WorkspaceNotFoundException;
import io.github.cherepavel.application.port.AnythingLlmGateway;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import static io.github.cherepavel.infrastructure.retrofit.ResponseMapper.fields;
import static io.github.cherepavel.infrastructure.retrofit.ResponseMapper.invalid;
import static io.github.cherepavel.infrastructure.retrofit.ResponseMapper.object;
import static io.github.cherepavel.infrastructure.retrofit.ResponseMapper.optional;
import static io.github.cherepavel.infrastructure.retrofit.ResponseMapper.reply;
import static io.github.cherepavel.infrastructure.retrofit.ResponseMapper.required;
import static io.github.cherepavel.infrastructure.retrofit.ResponseMapper.workspace;

/** HTTP adapter. Owns authentication, timeouts, status handling and SDK-created resources. */
public final class RetrofitAnythingLlmGateway implements AnythingLlmGateway, AutoCloseable {
    private final OkHttpClient transport;
    private final Endpoints api;
    private volatile boolean closed;

    public RetrofitAnythingLlmGateway(URI serverUri, String apiKey, Duration connectTimeout, Duration requestTimeout) {
        URI uri = serverUri;
        if (uri == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null || uri.getPort() > 65535) {
            throw new IllegalArgumentException("serverUri must be an HTTP(S) server URI without credentials, query or fragment");
        }
        String key = apiKey;
        if (key == null || key.isBlank() || key.regionMatches(true, 0, "Bearer ", 0, 7)
                || key.chars().anyMatch(c -> c <= 32 || c >= 127)) {
            throw new IllegalArgumentException("apiKey must be a nonblank ASCII key without Bearer prefix or whitespace");
        }
        validateTimeout(connectTimeout);
        validateTimeout(requestTimeout);
        String base = uri.toASCIIString().replaceAll("/+$", "");
        if (base.endsWith("/api") || base.endsWith("/api/v1")) {
            throw new IllegalArgumentException("serverUri must not include the API prefix");
        }
        transport = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout).callTimeout(requestTimeout)
                .readTimeout(requestTimeout).writeTimeout(requestTimeout)
                .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        .header("Authorization", "Bearer " + key)
                        .method(chain.request().method(), singleUse(chain.request().body())).build())).build();
        api = new Retrofit.Builder().baseUrl(base + "/api/").client(transport)
                .addConverterFactory(GsonConverterFactory.create()).build().create(Endpoints.class);
    }

    // OkHttp also has status-based follow-ups (e.g. 503 Retry-After: 0).
    // Mark mutation bodies as one-shot so even those paths cannot replay a POST.
    private static RequestBody singleUse(RequestBody body) {
        if (body == null) return null;
        return new RequestBody() {
            @Override public MediaType contentType() { return body.contentType(); }
            @Override public long contentLength() throws IOException { return body.contentLength(); }
            @Override public boolean isOneShot() { return true; }
            @Override public void writeTo(BufferedSink sink) throws IOException { body.writeTo(sink); }
        };
    }

    private static void validateTimeout(Duration value) {
        if (value == null || value.isNegative() || value.isZero()
                || value.compareTo(Duration.ofMillis(1)) < 0
                || value.compareTo(Duration.ofMillis(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("timeouts must be between 1ms and 2147483647ms");
        }
    }

    @Override public List<Workspace> listWorkspaces() throws AnythingLlmException {
        JsonElement entries = execute(api.workspaces(), "list workspaces").get("workspaces");
        if (entries == null || !entries.isJsonArray()) throw invalid("list workspaces");
        List<Workspace> result = new ArrayList<>();
        for (JsonElement entry : entries.getAsJsonArray()) result.add(workspace(object(entry)));
        return List.copyOf(result);
    }

    @Override public Workspace getWorkspace(String slug) throws AnythingLlmException {
        JsonElement value = execute(api.workspace(slug), "get workspace").get("workspace");
        if (value != null && value.isJsonArray()) {
            if (value.getAsJsonArray().isEmpty()) throw new WorkspaceNotFoundException("get workspace: no match");
            if (value.getAsJsonArray().size() != 1) throw invalid("get workspace");
            value = value.getAsJsonArray().get(0);
        }
        return workspace(object(value));
    }

    @Override public ThreadInfo createThread(String workspaceSlug, String name) throws AnythingLlmException {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        JsonObject value = object(execute(api.createThread(workspaceSlug, body), "create thread").get("thread"));
        return new ThreadInfo(optional(value, "id"), required(value, "slug"), required(value, "name"), fields(value));
    }

    @Override public ChatReply chatWorkspace(String slug, ChatRequest request) throws AnythingLlmException {
        return reply(execute(api.chat(slug, RequestMapper.chat(request)), "workspace chat"));
    }

    @Override public ChatReply chatThread(String workspaceSlug, String threadSlug, ChatRequest request)
            throws AnythingLlmException {
        return reply(execute(api.threadChat(workspaceSlug, threadSlug, RequestMapper.chat(request)), "thread chat"));
    }

    /** Releases only this client's SDK-created pool and executor. Does not await active operations. */
    @Override public void close() {
        closed = true;
        transport.dispatcher().cancelAll();
        transport.connectionPool().evictAll();
        transport.dispatcher().executorService().shutdown();
    }

    private JsonObject execute(Call<ResponseBody> call, String operation) throws AnythingLlmException {
        if (closed) throw new IllegalStateException("client is closed");
        final String json;
        try {
            var response = call.execute();
            try (ResponseBody error = response.errorBody(); ResponseBody body = response.body()) {
                if (!response.isSuccessful()) throw new HttpException(response.code(), operation);
                if (body == null) throw invalid(operation);
                json = body.string();
            }
        } catch (IOException e) {
            throw new TransportException(operation + ": transport failure", e);
        }
        return ResponseMapper.parse(json, operation);
    }

    interface Endpoints {
        @GET("v1/workspaces") Call<ResponseBody> workspaces();
        @GET("v1/workspace/{slug}") Call<ResponseBody> workspace(@Path("slug") String slug);
        @POST("v1/workspace/{slug}/thread/new") Call<ResponseBody> createThread(@Path("slug") String slug, @Body JsonObject body);
        @POST("v1/workspace/{slug}/chat") Call<ResponseBody> chat(@Path("slug") String slug, @Body JsonObject body);
        @POST("v1/workspace/{slug}/thread/{thread}/chat") Call<ResponseBody> threadChat(
                @Path("slug") String slug, @Path("thread") String thread, @Body JsonObject body);
    }
}
