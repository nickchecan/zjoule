package com.developer.nefarious.zjoule.plugin.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import org.bsc.langgraph4j.state.AppenderChannel;

import com.developer.nefarious.zjoule.plugin.agent.model.ModelProvider;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Summarizes the older part of a thread and drops it from the checkpoint.
 * <p>
 * LangGraph4j 1.9.7 has no RemoveMessage type. {@code RemoveByHash} matches on
 * hash code and deletes every equal message, so a repeated prompt would lose
 * both copies. The update replaces the channel with the kept tail instead.
 * A cancel before or after the summary call leaves the checkpoint unchanged.
 */
final class CompactAction {

    private CompactAction() {
    }

    /**
     * Returns the state update, or an empty map when compaction does not run.
     *
     * @param state the current thread state, including the new user message.
     * @param models the same provider the chat node calls.
     * @param cancelled reports the active turn. Checked before and after the summary call.
     * @param threshold the message count that triggers compaction.
     * @param keep how many recent messages to retain.
     * @return the channel update.
     */
    static Map<String, Object> apply(final ChatState state, final ModelProvider models,
            final BooleanSupplier cancelled, final IntSupplier threshold, final IntSupplier keep) {
        if (cancelled.getAsBoolean()) {
            return Map.of();
        }
        List<ChatMessage> messages = state.messages();
        int cut = HistoryCompaction.keepFrom(messages, threshold.getAsInt(), keep.getAsInt());
        if (cut < 0) {
            return Map.of();
        }
        String summary = summarize(models, state.summary(), messages.subList(0, cut), cancelled);
        if (summary == null) {
            return Map.of();
        }
        return replacement(summary, messages.subList(cut, messages.size()));
    }

    /**
     * Calls the blocking model and discards the text when the turn was cancelled.
     *
     * @param models the model provider.
     * @param previous the summary already stored.
     * @param older the prefix being folded.
     * @param cancelled the active turn.
     * @return the summary, or {@code null} when the turn was cancelled.
     */
    private static String summarize(final ModelProvider models, final String previous, final List<ChatMessage> older,
            final BooleanSupplier cancelled) {
        ChatResponse response;
        try {
            response = models.blocking().chat(HistoryCompaction.request(previous, older));
        } catch (RuntimeException ex) {
            if (cancelled.getAsBoolean()) {
                return null;
            }
            throw ex;
        }
        if (cancelled.getAsBoolean()) {
            return null;
        }
        return text(response);
    }

    /**
     * Reads the summary text.
     *
     * @param response the blocking response.
     * @return the text.
     */
    private static String text(final ChatResponse response) {
        AiMessage message = response == null ? null : response.aiMessage();
        if (message == null || message.text() == null || message.text().isBlank()) {
            throw new IllegalStateException("The summary was empty");
        }
        return message.text();
    }

    /**
     * Builds the checkpoint update for a completed summary.
     *
     * @param summary the new summary.
     * @param kept the messages that stay.
     * @return the state update.
     */
    private static Map<String, Object> replacement(final String summary, final List<ChatMessage> kept) {
        Map<String, Object> update = new HashMap<>();
        update.put(ChatState.MESSAGES, AppenderChannel.ReplaceAllWith.of(new ArrayList<>(kept)));
        update.put(ChatState.SUMMARY, summary);
        update.put(ChatState.SUMMARIZED_COUNT, Integer.valueOf(0));
        return update;
    }
}
