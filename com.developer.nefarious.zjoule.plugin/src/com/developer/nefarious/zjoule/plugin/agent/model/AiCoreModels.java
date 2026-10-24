package com.developer.nefarious.zjoule.plugin.agent.model;

import java.util.Map;
import java.util.function.Supplier;

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

/**
 * Builds LangChain4j clients for SAP AI Core and Ollama.
 * The JDK HTTP client is set explicitly because ServiceLoader is unreliable under OSGi.
 */
public final class AiCoreModels {

    private static final String AUTHORIZATION = "Authorization";

    private static final String RESOURCE_GROUP = "AI-Resource-Group";

    private static final String API_VERSION = "api-version";

    private static final String BEARER = "Bearer ";

    private AiCoreModels() {
    }

    /**
     * Builds a streaming AI Core client. No API key is set, so the client does not add its own
     * Authorization header.
     *
     * @param target the deployment.
     * @param token supplies a bearer token for each request.
     * @return the streaming model.
     */
    public static StreamingChatModel streaming(final AiCoreTarget target, final Supplier<String> token) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(target.deploymentBaseUrl())
                .modelName(target.modelName())
                .customHeaders(headers(target, token))
                .customQueryParams(query(target))
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
    }

    /**
     * Builds a blocking AI Core client for a later compact node.
     *
     * @param target the deployment.
     * @param token supplies a bearer token for each request.
     * @return the blocking model.
     */
    public static ChatModel blocking(final AiCoreTarget target, final Supplier<String> token) {
        return OpenAiChatModel.builder()
                .baseUrl(target.deploymentBaseUrl())
                .modelName(target.modelName())
                .customHeaders(headers(target, token))
                .customQueryParams(query(target))
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
    }

    /**
     * Builds a streaming Ollama client.
     *
     * @param endpoint the Ollama base URL.
     * @param model the model name.
     * @return the streaming model.
     */
    public static StreamingChatModel ollamaStreaming(final String endpoint, final String model) {
        return OllamaStreamingChatModel.builder()
                .baseUrl(endpoint)
                .modelName(model)
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
    }

    /**
     * Builds a blocking Ollama client.
     *
     * @param endpoint the Ollama base URL.
     * @param model the model name.
     * @return the blocking model.
     */
    public static ChatModel ollamaBlocking(final String endpoint, final String model) {
        return OllamaChatModel.builder()
                .baseUrl(endpoint)
                .modelName(model)
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
    }

    /**
     * Headers are computed per request so a refreshed token is picked up.
     *
     * @param target the deployment.
     * @param token the token supplier.
     * @return the header supplier.
     */
    private static Supplier<Map<String, String>> headers(final AiCoreTarget target, final Supplier<String> token) {
        return () -> Map.of(AUTHORIZATION, BEARER + token.get(), RESOURCE_GROUP, target.resourceGroup());
    }

    /**
     * Query parameters for the AI Core inference URL.
     *
     * @param target the deployment.
     * @return the query map.
     */
    private static Map<String, String> query(final AiCoreTarget target) {
        return Map.of(API_VERSION, target.apiVersion());
    }
}
