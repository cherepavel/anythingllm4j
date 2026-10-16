package io.github.cherepavel;

import io.github.cherepavel.application.api.Workspaces;
import io.github.cherepavel.application.api.Threads;
import io.github.cherepavel.application.service.WorkspaceService;
import io.github.cherepavel.application.service.ThreadService;
import io.github.cherepavel.infrastructure.retrofit.RetrofitAnythingLlmGateway;
import java.net.URI;
import java.time.Duration;

/** SDK entry point and composition root. Owns its transport; reuse until close(). */
public final class AnythingLlm implements AutoCloseable {
    private final RetrofitAnythingLlmGateway gateway;
    private final Workspaces workspaces;
    private final Threads threads;

    private AnythingLlm(Builder builder) {
        gateway = new RetrofitAnythingLlmGateway(builder.serverUri, builder.apiKey,
                builder.connectTimeout, builder.requestTimeout);
        workspaces = new WorkspaceService(gateway);
        threads = new ThreadService(gateway);
    }

    public static Builder builder() { return new Builder(); }
    public Workspaces workspaces() { return workspaces; }
    public Threads threads() { return threads; }
    @Override public void close() { gateway.close(); }

    /** Settings are validated by build(); credentials are never rendered by this builder. */
    public static final class Builder {
        private URI serverUri;
        private String apiKey;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofMinutes(5);
        private Builder() { }
        public Builder serverUri(URI value) { serverUri = value; return this; }
        public Builder apiKey(String value) { apiKey = value; return this; }
        public Builder connectTimeout(Duration value) { connectTimeout = value; return this; }
        public Builder requestTimeout(Duration value) { requestTimeout = value; return this; }
        public AnythingLlm build() { return new AnythingLlm(this); }
    }

}
