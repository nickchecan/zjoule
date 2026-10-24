package com.developer.nefarious.zjoule.plugin.agent.persistence;

import java.util.List;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * Builds the short title stored in the session index.
 */
final class SessionTitles {

    /** Maximum title length. */
    static final int MAX_LENGTH = 60;

    /** Title used before the first user message. */
    static final String PLACEHOLDER = SessionInfo.NEW_CHAT_TITLE;

    /** Title used when the checkpoint file cannot be read. */
    static final String UNREADABLE = "Unreadable session";

    private SessionTitles() {
    }

    /**
     * Collapses whitespace and limits the length.
     *
     * @param text the first user message.
     * @return the title.
     */
    static String fromUserText(final String text) {
        if (text == null) {
            return PLACEHOLDER;
        }
        String collapsed = text.replaceAll("\\s+", " ").trim();
        if (collapsed.isEmpty()) {
            return PLACEHOLDER;
        }
        if (collapsed.length() <= MAX_LENGTH) {
            return collapsed;
        }
        return collapsed.substring(0, MAX_LENGTH);
    }

    /**
     * Uses the first user message as the title.
     *
     * @param messages the checkpoint messages.
     * @return the title.
     */
    static String fromMessages(final List<ChatMessage> messages) {
        if (messages == null) {
            return PLACEHOLDER;
        }
        for (ChatMessage message : messages) {
            if (message instanceof UserMessage user) {
                return fromUserText(user.singleText());
            }
        }
        return PLACEHOLDER;
    }

    /**
     * Returns whether a later user message may replace the title.
     *
     * @param title the current title.
     * @return {@code true} when the title is still a placeholder.
     */
    static boolean replaceable(final String title) {
        return title == null || title.isBlank() || PLACEHOLDER.equals(title) || UNREADABLE.equals(title);
    }
}
