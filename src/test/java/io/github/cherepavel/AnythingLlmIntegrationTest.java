package io.github.cherepavel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import retrofit2.Call;
import static org.junit.jupiter.api.Assertions.*;

/** Live contract tests, enabled only when an API key is supplied explicitly. */
@EnabledIfEnvironmentVariable(named = "ANYTHINGLLM_API_KEY", matches = ".+")
class AnythingLlmIntegrationTest {
    private AnythingLlmApi api;

    @BeforeEach
    void connect() {
        String url = System.getenv().getOrDefault("ANYTHINGLLM_API_URL", "http://localhost:3001/api/");
        api = AnythingLlmClient.create(url, System.getenv("ANYTHINGLLM_API_KEY"));
    }

    @Test
    void authenticates() throws IOException {
        assertTrue(json(api.authenticate()).getAsJsonObject().get("authenticated").getAsBoolean());
    }

    @Test
    void listsWorkspaces() throws IOException {
        assertTrue(json(api.workspaces()).getAsJsonObject().get("workspaces").isJsonArray());
    }

    @Test
    void listsDocuments() throws IOException {
        assertTrue(json(api.documents(Map.of())).isJsonObject());
    }

    @Test
    void readsDocumentContracts() throws IOException {
        assertTrue(json(api.acceptedFileTypes()).getAsJsonObject().get("types").isJsonObject());
        assertTrue(json(api.metadataSchema()).getAsJsonObject().get("schema").isJsonObject());
    }

    @Test
    void readsMultiUserMode() throws IOException {
        assertTrue(json(api.isMultiUserMode()).getAsJsonObject().get("isMultiUser").isJsonPrimitive());
    }

    @Test
    void listsOpenAiModelsAndVectorStores() throws IOException {
        assertTrue(json(api.models()).getAsJsonObject().get("data").isJsonArray());
        assertTrue(json(api.vectorStores()).getAsJsonObject().get("data").isJsonArray());
    }

    @Test
    void createsUpdatesAndDeletesOwnWorkspaceAndThread() throws IOException {
        String name = "sdk-integration-" + UUID.randomUUID();
        JsonObject create = new JsonObject();
        create.addProperty("name", name);
        JsonObject workspace = json(api.createWorkspace(create)).getAsJsonObject().getAsJsonObject("workspace");
        String slug = workspace.get("slug").getAsString();
        try {
            assertTrue(json(api.workspace(slug)).getAsJsonObject().has("workspace"));
            JsonObject update = new JsonObject();
            update.addProperty("name", name + "-updated");
            assertEquals(name + "-updated", json(api.updateWorkspace(slug, update)).getAsJsonObject()
                    .getAsJsonObject("workspace").get("name").getAsString());
            assertTrue(json(api.workspaceChats(slug, Map.of("limit", "1", "orderBy", "asc")))
                    .getAsJsonObject().get("history").isJsonArray());
            JsonObject threadRequest = new JsonObject();
            threadRequest.addProperty("name", "SDK test thread");
            String threadSlug = json(api.createThread(slug, threadRequest)).getAsJsonObject()
                    .getAsJsonObject("thread").get("slug").getAsString();
            try {
                JsonObject rename = new JsonObject();
                rename.addProperty("name", "Renamed SDK test thread");
                assertEquals("Renamed SDK test thread", json(api.updateThread(slug, threadSlug, rename))
                        .getAsJsonObject().getAsJsonObject("thread").get("name").getAsString());
                assertTrue(json(api.threadChats(slug, threadSlug)).getAsJsonObject().get("history").isJsonArray());
            } finally {
                succeeds(api.deleteThread(slug, threadSlug));
            }
        } finally {
            succeeds(api.deleteWorkspace(slug));
        }
    }

    private static void succeeds(Call<Void> call) throws IOException {
        var response = call.execute();
        try (var error = response.errorBody()) {
            assertTrue(response.isSuccessful(), () -> "HTTP " + response.code());
        }
    }

    private static JsonElement json(Call<JsonElement> call) throws IOException {
        var response = call.execute();
        if (!response.isSuccessful()) {
            // Do not dump server settings, document contents, or credentials in test reports.
            try (var error = response.errorBody()) {
                fail(call.request().method() + " " + call.request().url().encodedPath()
                        + " returned HTTP " + response.code());
            }
        }
        return response.body();
    }
}
