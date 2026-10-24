package com.developer.nefarious.zjoule.plugin.agent.model;

import com.developer.nefarious.zjoule.plugin.auth.SessionManager;
import com.developer.nefarious.zjoule.plugin.memory.MemoryDeployment;
import com.developer.nefarious.zjoule.plugin.memory.MemoryOllamaEndpoint;
import com.developer.nefarious.zjoule.plugin.memory.MemoryOllamaModel;
import com.developer.nefarious.zjoule.plugin.memory.MemoryResourceGroup;
import com.developer.nefarious.zjoule.plugin.memory.MemoryServiceKey;
import com.developer.nefarious.zjoule.plugin.models.Deployment;
import com.developer.nefarious.zjoule.plugin.models.OllamaModel;
import com.developer.nefarious.zjoule.plugin.models.ServiceKey;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * Chooses SAP AI Core or Ollama from the current login.
 */
public final class LoginModelProvider implements ModelProvider {

    private static final String SAP = "sap";

    private static final String OLLAMA = "ollama";

    private final AiCoreTokenProvider tokens;

    /**
     * Creates the provider.
     *
     * @param tokens the in-memory AI Core token cache.
     */
    public LoginModelProvider(final AiCoreTokenProvider tokens) {
        this.tokens = tokens;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamingChatModel streaming() {
        return new RetryingStreamingChatModel(this::openStreaming, tokens::invalidate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatModel blocking() {
        return openBlocking();
    }

    /**
     * Returns {@code sap}, {@code ollama}, or an empty string.
     *
     * @return the provider id stored in the session index.
     */
    public String providerId() {
        if (SessionManager.isOllamaSession()) {
            return OLLAMA;
        }
        if (SessionManager.isSapSession()) {
            return SAP;
        }
        return "";
    }

    /**
     * Returns the model name for the current login, or an empty string.
     *
     * @return the model name.
     */
    public String modelName() {
        try {
            return readModelName();
        } catch (RuntimeException ex) {
            return "";
        }
    }

    /**
     * Opens a streaming client for the current login.
     *
     * @return the streaming model.
     */
    private StreamingChatModel openStreaming() {
        if (SessionManager.isOllamaSession()) {
            return AiCoreModels.ollamaStreaming(endpoint(), modelName());
        }
        if (SessionManager.isSapSession()) {
            return AiCoreModels.streaming(sapTarget(), tokens);
        }
        throw new IllegalStateException("Not logged in");
    }

    /**
     * Opens a blocking client for the current login.
     *
     * @return the blocking model.
     */
    private ChatModel openBlocking() {
        if (SessionManager.isOllamaSession()) {
            return AiCoreModels.ollamaBlocking(endpoint(), modelName());
        }
        if (SessionManager.isSapSession()) {
            return AiCoreModels.blocking(sapTarget(), tokens);
        }
        throw new IllegalStateException("Not logged in");
    }

    /**
     * Reads the model name without swallowing failures.
     *
     * @return the model name, or an empty string when no session is active.
     */
    private String readModelName() {
        if (SessionManager.isOllamaSession()) {
            return ollamaModelName();
        }
        if (SessionManager.isSapSession()) {
            return sapModelName();
        }
        return "";
    }

    /**
     * Returns the stored Ollama model id.
     *
     * @return the model id, or an empty string.
     */
    private String ollamaModelName() {
        OllamaModel model = MemoryOllamaModel.getInstance().load();
        if (model == null || model.getModel() == null) {
            return "";
        }
        return model.getModel();
    }

    /**
     * Returns the SAP deployment model name.
     *
     * @return the model name, or an empty string.
     */
    private String sapModelName() {
        Deployment deployment = MemoryDeployment.getInstance().load();
        if (deployment == null) {
            return "";
        }
        String name = deployment.getModelName();
        return name == null ? "" : name;
    }

    /**
     * Returns the stored Ollama endpoint.
     *
     * @return the endpoint.
     */
    private String endpoint() {
        String endpoint = MemoryOllamaEndpoint.getInstance().load();
        if (endpoint == null) {
            throw new IllegalStateException("Not logged in");
        }
        return endpoint;
    }

    /**
     * Builds the AI Core target from the secure key, resource group, and deployment.
     *
     * @return the target.
     */
    private AiCoreTarget sapTarget() {
        ServiceKey key = MemoryServiceKey.getInstance().load();
        Deployment deployment = MemoryDeployment.getInstance().load();
        String group = MemoryResourceGroup.getInstance().load();
        if (key == null || deployment == null) {
            throw new IllegalStateException("Not logged in");
        }
        return new AiCoreTarget(key.getServiceURL(), deployment.getId(), group, sapModelName(),
                AiCoreTarget.API_VERSION);
    }
}
