package com.developer.nefarious.zjoule.plugin.agent;

import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;

/**
 * Decides which messages a compact node folds into the summary.
 * <p>
 * The cut moves backward when it would start the kept tail on a tool result,
 * so the assistant tool request stays with that result. Compaction is skipped
 * when that adjustment would leave nothing to summarize.
 */
public final class HistoryCompaction {

    /** Returned when the history should be left unchanged. */
    public static final int NO_CUT = -1;

    private static final String INSTRUCTION = "Summarize the conversation so a later reply can continue it. "
            + "Keep names, decisions, and code the user still needs. Reply with the summary only.";

    private HistoryCompaction() {
    }

    /**
     * Returns the first index to keep, or {@link #NO_CUT} when compaction should not run.
     *
     * @param messages the checkpoint messages, oldest first.
     * @param threshold compaction runs only when the list is longer than this.
     * @param keep how many recent messages to retain.
     * @return the exclusive end of the summarized prefix.
     */
    public static int keepFrom(final List<?> messages, final int threshold, final int keep) {
        if (messages == null || threshold <= 0 || messages.size() <= threshold) {
            return NO_CUT;
        }
        int cut = messages.size() - retainCount(threshold, keep);
        if (cut <= 0) {
            return NO_CUT;
        }
        while (cut > 0 && messages.get(cut) instanceof ToolExecutionResultMessage) {
            cut--;
        }
        if (cut <= 0) {
            return NO_CUT;
        }
        return cut;
    }

    /**
     * Builds the blocking summary request for one prefix.
     *
     * @param previousSummary the summary already stored, or blank.
     * @param older the messages being folded. They are not sent as chat history.
     * @return the request for {@code ModelProvider.blocking()}.
     */
    public static ChatRequest request(final String previousSummary, final List<?> older) {
        StringBuilder body = new StringBuilder(INSTRUCTION);
        if (previousSummary != null && !previousSummary.isBlank()) {
            body.append("\nPrevious summary:\n").append(previousSummary);
        }
        body.append("\nConversation:\n").append(transcript(older));
        List<ChatMessage> prompt = new ArrayList<>();
        prompt.add(UserMessage.from(body.toString()));
        return ChatRequest.builder().messages(prompt).build();
    }

    /**
     * How many trailing messages stay when a run actually compacts.
     *
     * @param threshold the trigger count.
     * @param keep the configured keep count.
     * @return a keep count strictly below the threshold.
     */
    private static int retainCount(final int threshold, final int keep) {
        if (keep > 0 && keep < threshold) {
            return keep;
        }
        return Math.max(1, threshold / 2);
    }

    /**
     * Renders messages as plain lines. Tool calls are not forwarded raw.
     *
     * @param older the messages being summarized.
     * @return the transcript.
     */
    private static String transcript(final List<?> older) {
        StringBuilder text = new StringBuilder();
        if (older == null) {
            return "";
        }
        for (Object message : older) {
            text.append(line(message)).append('\n');
        }
        return text.toString();
    }

    /**
     * Renders one message for the summarizer.
     *
     * @param message a checkpoint message.
     * @return one line.
     */
    private static String line(final Object message) {
        if (message instanceof UserMessage user) {
            return "User: " + user.singleText();
        }
        if (message instanceof AiMessage ai) {
            return assistantLine(ai);
        }
        if (message instanceof ToolExecutionResultMessage tool) {
            return "Tool: " + tool.text();
        }
        if (message instanceof ChatMessage chat) {
            return chat.type().name();
        }
        return "";
    }

    /**
     * Renders an assistant message, including one that only requested a tool.
     *
     * @param message the assistant message.
     * @return one line.
     */
    private static String assistantLine(final AiMessage message) {
        if (message.text() != null && !message.text().isBlank()) {
            return "Assistant: " + message.text();
        }
        return "Assistant: tool call";
    }
}
