package io.github.cherepavel;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

/** AnythingLLM REST API. Paths are relative to the instance /api/ URL. */
public interface AnythingLlmApi {

    /** GET /api/v1/auth. */
    @GET("v1/auth")
    Call<JsonElement> authenticate();

    /** GET /api/v1/admin/is-multi-user-mode. */
    @GET("v1/admin/is-multi-user-mode")
    Call<JsonElement> isMultiUserMode();

    /** GET /api/v1/admin/users. */
    @GET("v1/admin/users")
    Call<JsonElement> adminUsers();

    /** POST /api/v1/admin/users/new. */
    @POST("v1/admin/users/new")
    Call<JsonElement> createUser(@Body JsonElement body);

    /** POST /api/v1/admin/users/{id}. */
    @POST("v1/admin/users/{id}")
    Call<JsonElement> updateUser(@Path("id") String id, @Body JsonElement body);

    /** DELETE /api/v1/admin/users/{id}. */
    @DELETE("v1/admin/users/{id}")
    Call<JsonElement> deleteUser(@Path("id") String id);

    /** GET /api/v1/admin/invites. */
    @GET("v1/admin/invites")
    Call<JsonElement> invites();

    /** POST /api/v1/admin/invite/new. */
    @POST("v1/admin/invite/new")
    Call<JsonElement> createInvite(@Body JsonElement body);

    /** DELETE /api/v1/admin/invite/{id}. */
    @DELETE("v1/admin/invite/{id}")
    Call<JsonElement> deleteInvite(@Path("id") String id);

    /** GET /api/v1/admin/workspaces/{workspaceId}/users. */
    @GET("v1/admin/workspaces/{workspaceId}/users")
    Call<JsonElement> workspaceUsers(@Path("workspaceId") String workspaceId);

    /** POST /api/v1/admin/workspaces/{workspaceId}/update-users. */
    @POST("v1/admin/workspaces/{workspaceId}/update-users")
    Call<JsonElement> updateWorkspaceUsers(@Path("workspaceId") String workspaceId, @Body JsonElement body);

    /** POST /api/v1/admin/workspaces/{workspaceSlug}/manage-users. */
    @POST("v1/admin/workspaces/{workspaceSlug}/manage-users")
    Call<JsonElement> manageWorkspaceUsers(@Path("workspaceSlug") String workspaceSlug, @Body JsonElement body);

    /** POST /api/v1/admin/workspace-chats. */
    @POST("v1/admin/workspace-chats")
    Call<JsonElement> adminChats(@Body JsonElement body);

    /** POST /api/v1/admin/preferences. */
    @POST("v1/admin/preferences")
    Call<JsonElement> updatePreferences(@Body JsonElement body);

    /** POST /api/v1/document/upload. */
    @Multipart
    @POST("v1/document/upload")
    Call<JsonElement> uploadDocument(@Part MultipartBody.Part file, @Part("addToWorkspaces") RequestBody workspaces, @Part("metadata") RequestBody metadata);

    /** POST /api/v1/document/upload/{folderName}. */
    @Multipart
    @POST("v1/document/upload/{folderName}")
    Call<JsonElement> uploadDocumentToFolder(@Path("folderName") String folderName, @Part MultipartBody.Part file, @Part("addToWorkspaces") RequestBody workspaces, @Part("metadata") RequestBody metadata);

    /** POST /api/v1/document/upload-link. */
    @POST("v1/document/upload-link")
    Call<JsonElement> uploadLink(@Body JsonElement body);

    /** POST /api/v1/document/raw-text. */
    @POST("v1/document/raw-text")
    Call<JsonElement> uploadRawText(@Body JsonElement body);

    /** GET /api/v1/documents. */
    @GET("v1/documents")
    Call<JsonElement> documents(@QueryMap Map<String, String> query);

    /** GET /api/v1/documents/folder/{folderName}. */
    @GET("v1/documents/folder/{folderName}")
    Call<JsonElement> folderDocuments(@Path("folderName") String folderName, @QueryMap Map<String, String> query);

    /** GET /api/v1/document/accepted-file-types. */
    @GET("v1/document/accepted-file-types")
    Call<JsonElement> acceptedFileTypes();

    /** GET /api/v1/document/metadata-schema. */
    @GET("v1/document/metadata-schema")
    Call<JsonElement> metadataSchema();

    /** GET /api/v1/document/{docName}. */
    @GET("v1/document/{docName}")
    Call<JsonElement> document(@Path("docName") String docName);

    /** POST /api/v1/document/create-folder. */
    @POST("v1/document/create-folder")
    Call<JsonElement> createFolder(@Body JsonElement body);

    /** DELETE /api/v1/document/remove-folder. */
    @HTTP(method = "DELETE", path = "v1/document/remove-folder", hasBody = true)
    Call<JsonElement> removeFolder(@Body JsonElement body);

    /** POST /api/v1/document/move-files. */
    @POST("v1/document/move-files")
    Call<JsonElement> moveFiles(@Body JsonElement body);

    /** GET /api/v1/document/generated-files/{filename}. */
    @Streaming
    @GET("v1/document/generated-files/{filename}")
    Call<ResponseBody> generatedFile(@Path("filename") String filename);

    /** POST /api/v1/workspace/new. */
    @POST("v1/workspace/new")
    Call<JsonElement> createWorkspace(@Body JsonElement body);

    /** GET /api/v1/workspaces. */
    @GET("v1/workspaces")
    Call<JsonElement> workspaces();

    /** GET /api/v1/workspace/{slug}. */
    @GET("v1/workspace/{slug}")
    Call<JsonElement> workspace(@Path("slug") String slug);

    /** DELETE /api/v1/workspace/{slug}. */
    @DELETE("v1/workspace/{slug}")
    Call<Void> deleteWorkspace(@Path("slug") String slug);

    /** POST /api/v1/workspace/{slug}/update. */
    @POST("v1/workspace/{slug}/update")
    Call<JsonElement> updateWorkspace(@Path("slug") String slug, @Body JsonElement body);

    /** GET /api/v1/workspace/{slug}/chats. */
    @GET("v1/workspace/{slug}/chats")
    Call<JsonElement> workspaceChats(@Path("slug") String slug, @QueryMap Map<String, String> query);

    /** POST /api/v1/workspace/{slug}/update-embeddings. */
    @POST("v1/workspace/{slug}/update-embeddings")
    Call<JsonElement> updateEmbeddings(@Path("slug") String slug, @Body JsonElement body);

    /** POST /api/v1/workspace/{slug}/update-pin. */
    @POST("v1/workspace/{slug}/update-pin")
    Call<JsonElement> updatePin(@Path("slug") String slug, @Body JsonElement body);

    /** POST /api/v1/workspace/{slug}/chat. */
    @POST("v1/workspace/{slug}/chat")
    Call<ChatResponse> chat(@Path("slug") String slug, @Body ChatRequest body);

    /** POST /api/v1/workspace/{slug}/stream-chat. */
    @Streaming
    @POST("v1/workspace/{slug}/stream-chat")
    Call<ResponseBody> streamChat(@Path("slug") String slug, @Body ChatRequest body);

    /** POST /api/v1/workspace/{slug}/vector-search. */
    @POST("v1/workspace/{slug}/vector-search")
    Call<JsonElement> vectorSearch(@Path("slug") String slug, @Body JsonElement body);

    /** GET /api/v1/system/env-dump. */
    @Streaming
    @GET("v1/system/env-dump")
    Call<ResponseBody> dumpEnvironment();

    /** GET /api/v1/system. */
    @GET("v1/system")
    Call<JsonElement> systemSettings();

    /** GET /api/v1/system/vector-count. */
    @GET("v1/system/vector-count")
    Call<JsonElement> vectorCount();

    /** POST /api/v1/system/update-env. */
    @POST("v1/system/update-env")
    Call<JsonElement> updateEnvironment(@Body JsonElement body);

    /** GET /api/v1/system/export-chats. */
    @Streaming
    @GET("v1/system/export-chats")
    Call<ResponseBody> exportChats(@QueryMap Map<String, String> query);

    /** DELETE /api/v1/system/remove-documents. */
    @HTTP(method = "DELETE", path = "v1/system/remove-documents", hasBody = true)
    Call<JsonElement> removeDocuments(@Body JsonElement body);

    /** POST /api/v1/workspace/{slug}/thread/new. */
    @POST("v1/workspace/{slug}/thread/new")
    Call<JsonElement> createThread(@Path("slug") String slug, @Body JsonElement body);

    /** POST /api/v1/workspace/{slug}/thread/{threadSlug}/update. */
    @POST("v1/workspace/{slug}/thread/{threadSlug}/update")
    Call<JsonElement> updateThread(@Path("slug") String slug, @Path("threadSlug") String threadSlug, @Body JsonElement body);

    /** DELETE /api/v1/workspace/{slug}/thread/{threadSlug}. */
    @DELETE("v1/workspace/{slug}/thread/{threadSlug}")
    Call<Void> deleteThread(@Path("slug") String slug, @Path("threadSlug") String threadSlug);

    /** GET /api/v1/workspace/{slug}/thread/{threadSlug}/chats. */
    @GET("v1/workspace/{slug}/thread/{threadSlug}/chats")
    Call<JsonElement> threadChats(@Path("slug") String slug, @Path("threadSlug") String threadSlug);

    /** POST /api/v1/workspace/{slug}/thread/{threadSlug}/chat. */
    @POST("v1/workspace/{slug}/thread/{threadSlug}/chat")
    Call<ChatResponse> threadChat(@Path("slug") String slug, @Path("threadSlug") String threadSlug, @Body ChatRequest body);

    /** POST /api/v1/workspace/{slug}/thread/{threadSlug}/stream-chat. */
    @Streaming
    @POST("v1/workspace/{slug}/thread/{threadSlug}/stream-chat")
    Call<ResponseBody> streamThreadChat(@Path("slug") String slug, @Path("threadSlug") String threadSlug, @Body ChatRequest body);

    /** GET /api/v1/users. */
    @GET("v1/users")
    Call<JsonElement> users();

    /** GET /api/v1/openai/models. */
    @GET("v1/openai/models")
    Call<JsonElement> models();

    /** POST /api/v1/openai/chat/completions. */
    @Streaming
    @POST("v1/openai/chat/completions")
    Call<ResponseBody> chatCompletions(@Body JsonElement body);

    /** POST /api/v1/openai/images/generations. */
    @Multipart
    @POST("v1/openai/images/generations")
    Call<ResponseBody> generateImages(@Part("prompt") RequestBody prompt, @Part("size") RequestBody size, @Part("response_format") RequestBody format, @Part List<MultipartBody.Part> references);

    /** POST /api/v1/openai/embeddings. */
    @POST("v1/openai/embeddings")
    Call<JsonElement> embeddings(@Body JsonElement body);

    /** GET /api/v1/openai/vector_stores. */
    @GET("v1/openai/vector_stores")
    Call<JsonElement> vectorStores();

    /** GET /api/v1/embed. */
    @GET("v1/embed")
    Call<JsonElement> embeds();

    /** GET /api/v1/embed/{embedUuid}/chats. */
    @GET("v1/embed/{embedUuid}/chats")
    Call<JsonElement> embedChats(@Path("embedUuid") String embedUuid);

    /** GET /api/v1/embed/{embedUuid}/chats/{sessionUuid}. */
    @GET("v1/embed/{embedUuid}/chats/{sessionUuid}")
    Call<JsonElement> embedSessionChats(@Path("embedUuid") String embedUuid, @Path("sessionUuid") String sessionUuid);

    /** POST /api/v1/embed/new. */
    @POST("v1/embed/new")
    Call<JsonElement> createEmbed(@Body JsonElement body);

    /** POST /api/v1/embed/{embedUuid}. */
    @POST("v1/embed/{embedUuid}")
    Call<JsonElement> updateEmbed(@Path("embedUuid") String embedUuid, @Body JsonElement body);

    /** DELETE /api/v1/embed/{embedUuid}. */
    @DELETE("v1/embed/{embedUuid}")
    Call<JsonElement> deleteEmbed(@Path("embedUuid") String embedUuid);

    /** POST /api/v1/transcription/transcribe. */
    @Multipart
    @POST("v1/transcription/transcribe")
    Call<JsonElement> transcribe(@Part MultipartBody.Part audio);
}
