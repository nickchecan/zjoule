package com.developer.nefarious.zjoule.plugin.agent.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * Resolves the chat model from the current login on each call.
 */
public interface ModelProvider {

    /**
     * Returns the streaming model for the current login.
     *
     * @return the streaming model.
     * @throws IllegalStateException when the user is not logged in.
     */
    StreamingChatModel streaming();

    /**
     * Returns the blocking model used by the compact node.
     * It uses the same login, deployment, and model as {@link #streaming()}.
     *
     * @return the blocking model.
     * @throws IllegalStateException when the user is not logged in.
     */
    ChatModel blocking();
}
