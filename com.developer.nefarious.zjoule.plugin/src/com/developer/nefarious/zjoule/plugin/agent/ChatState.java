package com.developer.nefarious.zjoule.plugin.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bsc.langgraph4j.langchain4j.serializer.jackson.LC4jJacksonStateSerializer;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import dev.langchain4j.data.message.ChatMessage;

/**
 * Graph state for one chat thread.
 * <p>
 * The messages channel keeps duplicates. LangChain4j messages are value-equal,
 * and the default messages schema would drop a repeated prompt.
 */
public final class ChatState extends MessagesState<ChatMessage> {

    /** Full conversation history. */
    public static final String MESSAGES = "messages";

    /** Rolling summary. Reserved for a later compact node. */
    public static final String SUMMARY = "summary";

    /** Number of leading messages covered by {@link #SUMMARY}. */
    public static final String SUMMARIZED_COUNT = "summarized_count";

    /** Human approval value. Reserved for a later tools node. */
    public static final String APPROVAL = "approval";

    /** Value written when the user approves a tool call. */
    public static final String APPROVED = "APPROVED";

    /** Value written when the user rejects a tool call. */
    public static final String REJECTED = "REJECTED";

    /** Channel schema. Does not reuse {@code MessagesState.SCHEMA}. */
    public static final Map<String, Channel<?>> SCHEMA = schema();

    /**
     * Creates state from the checkpoint map.
     *
     * @param data checkpoint values.
     */
    public ChatState(final Map<String, Object> data) {
        super(data);
    }

    /**
     * Returns the stored summary, or an empty string.
     *
     * @return the summary text.
     */
    public String summary() {
        return value(SUMMARY, "");
    }

    /**
     * Returns how many leading messages the summary already covers.
     *
     * @return a zero-based message index.
     */
    public int summarizedCount() {
        Object raw = data().get(SUMMARIZED_COUNT);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    /**
     * Returns the reserved approval value when one is present.
     *
     * @return the approval value.
     */
    public Optional<String> approval() {
        return value(APPROVAL);
    }

    /**
     * Returns the serializer used for checkpoints and graph state.
     *
     * @return a LangChain4j Jackson serializer.
     */
    public static StateSerializer<ChatState> serializer() {
        return new LC4jJacksonStateSerializer<>(ChatState::new);
    }

    /**
     * Builds the channel map.
     *
     * @return an immutable schema.
     */
    private static Map<String, Channel<?>> schema() {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put(MESSAGES, Channels.appenderWithDuplicate(ArrayList::new));
        channels.put(SUMMARY, Channels.base(() -> ""));
        channels.put(SUMMARIZED_COUNT, Channels.base(() -> Integer.valueOf(0)));
        channels.put(APPROVAL, Channels.base(() -> ""));
        return Map.copyOf(channels);
    }
}
