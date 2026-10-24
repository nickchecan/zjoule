package com.developer.nefarious.zjoule.plugin.agent.persistence;

import com.google.gson.annotations.SerializedName;

/**
 * Metadata for one chat session.
 *
 * @param threadId session id
 * @param title display title
 * @param createdAtMs creation time in epoch milliseconds
 * @param updatedAtMs last update in epoch milliseconds
 * @param provider {@code sap}, {@code ollama}, or blank after an index rebuild
 * @param model model name, or blank after an index rebuild
 */
public record SessionInfo(String threadId, String title, @SerializedName("createdAt") long createdAtMs,
        @SerializedName("updatedAt") long updatedAtMs, String provider, String model) {

    /** Title used until the first user message is known. */
    public static final String NEW_CHAT_TITLE = "New chat";
}
