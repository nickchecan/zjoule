package com.developer.nefarious.zjoule.plugin.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.bsc.langgraph4j.RunnableConfig;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

/**
 * Builds the prompt for one model call. The prompt is not stored.
 */
public final class PromptAssembler {

    /** RunnableConfig metadata key for the active editor text. */
    public static final String META_EDITOR_CONTEXT = "zjoule.editorContext";

    /** Prefix used when editor text is attached to the system message. */
    static final String EDITOR_PREFIX = " Consider the following code as context: ";

    private final Supplier<String> instructions;

    private final IntSupplier windowSize;

    /**
     * Creates the assembler.
     *
     * @param instructions the preference instructions.
     * @param windowSize how many trailing messages to send.
     */
    public PromptAssembler(final Supplier<String> instructions, final IntSupplier windowSize) {
        this.instructions = instructions;
        this.windowSize = windowSize;
    }

    /**
     * Builds the system message plus the visible history window.
     *
     * @param state the current thread state.
     * @param config the run config. Editor text is read from metadata.
     * @return the messages for this call.
     */
    public List<ChatMessage> build(final ChatState state, final RunnableConfig config) {
        List<ChatMessage> history = state.messages();
        int start = windowStart(history, state.summarizedCount(), windowSize.getAsInt());
        List<ChatMessage> prompt = new ArrayList<>();
        prompt.add(SystemMessage.from(systemText(state, config)));
        prompt.addAll(history.subList(start, history.size()));
        return List.copyOf(prompt);
    }

    /**
     * Joins instructions, editor context, and the reserved summary.
     *
     * @param state the current state.
     * @param config the run config.
     * @return the system text.
     */
    private String systemText(final ChatState state, final RunnableConfig config) {
        String base = instructions.get();
        StringBuilder text = new StringBuilder(base == null ? "" : base);
        String editor = editorContext(config);
        if (editor != null) {
            text.append(EDITOR_PREFIX).append(editor);
        }
        appendSummary(text, state.summary());
        return text.toString();
    }

    /**
     * Appends a non-blank summary.
     *
     * @param text the system text.
     * @param summary the stored summary.
     */
    private static void appendSummary(final StringBuilder text, final String summary) {
        if (summary != null && !summary.isBlank()) {
            text.append('\n').append(summary);
        }
    }

    /**
     * Reads editor text from config metadata.
     * A missing value means there is no editor. An empty string is still context.
     *
     * @param config the run config.
     * @return the editor text, or {@code null} when it was not supplied.
     */
    private static String editorContext(final RunnableConfig config) {
        if (config == null) {
            return null;
        }
        Object value = config.metadata(META_EDITOR_CONTEXT).orElse(null);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Chooses the first history index included in the window.
     *
     * @param history the full history.
     * @param summarizedCount messages already covered by the summary.
     * @param window how many trailing messages to keep.
     * @return the inclusive start index.
     */
    static int windowStart(final List<ChatMessage> history, final int summarizedCount, final int window) {
        int size = history.size();
        int fromTail = Math.max(0, size - Math.max(window, 0));
        int start = Math.max(0, Math.max(summarizedCount, fromTail));
        if (start > size) {
            start = size;
        }
        return advance(history, start);
    }

    /**
     * Moves the start forward past a tool result so a pair is not split.
     * An AI tool request that is already inside the window stays put.
     *
     * @param history the full history.
     * @param start the candidate index.
     * @return the adjusted index.
     */
    private static int advance(final List<ChatMessage> history, final int start) {
        int index = start;
        while (index < history.size() && startsOnToolResult(history.get(index))) {
            index++;
        }
        return index;
    }

    /**
     * Returns whether this message is a tool result the window must not open on.
     *
     * @param message the candidate first message.
     * @return {@code true} for a tool result.
     */
    private static boolean startsOnToolResult(final ChatMessage message) {
        if (message instanceof ToolExecutionResultMessage) {
            return true;
        }
        return false;
    }
}
