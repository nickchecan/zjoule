package com.developer.nefarious.zjoule.plugin.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.bsc.langgraph4j.RunnableConfig;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;

/**
 * Builds the prompt for one model call. The prompt is not stored.
 * <p>
 * Every message still in the checkpoint is sent. Compaction, not a fixed
 * window, is what shortens that list.
 */
public final class PromptAssembler {

    /** RunnableConfig metadata key for the active editor text. */
    public static final String META_EDITOR_CONTEXT = "zjoule.editorContext";

    /** Prefix used when editor text is attached to the system message. */
    static final String EDITOR_PREFIX = " Consider the following code as context: ";

    private final Supplier<String> instructions;

    /**
     * Creates the assembler.
     *
     * @param instructions the preference instructions.
     */
    public PromptAssembler(final Supplier<String> instructions) {
        this.instructions = instructions;
    }

    /**
     * Builds the system message plus the messages still in the checkpoint.
     *
     * @param state the current thread state.
     * @param config the run config. Editor text is read from metadata.
     * @return the messages for this call.
     */
    public List<ChatMessage> build(final ChatState state, final RunnableConfig config) {
        List<ChatMessage> prompt = new ArrayList<>();
        prompt.add(SystemMessage.from(systemText(state, config)));
        prompt.addAll(state.messages());
        return List.copyOf(prompt);
    }

    /**
     * Joins instructions, editor context, and the stored summary.
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
}
