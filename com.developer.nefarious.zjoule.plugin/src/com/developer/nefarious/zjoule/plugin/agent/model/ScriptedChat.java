package com.developer.nefarious.zjoule.plugin.agent.model;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.StreamingHandle;

/**
 * Deterministic model used by the SWTBot suite.
 * <p>
 * It is active only when {@code zjoule.e2e.scripted} is true. Production
 * launches never set that property. {@code zjoule.e2e.holdStreams} parks the
 * streaming reply until {@link StreamingHandle#cancel()} so the Stop action
 * can be driven without a network.
 */
public final class ScriptedChat {

    /** System property that selects this provider. */
    public static final String PROPERTY = "zjoule.e2e.scripted";

    /** System property that parks streaming replies until cancel. */
    public static final String HOLD = "zjoule.e2e.holdStreams";

    /** Set to {@code true} once a held stream has published its cancel handle. */
    public static final String ARMED = "zjoule.e2e.streamArmed";

    /** Reply text for every streamed turn. */
    public static final String REPLY = "Hello there";

    /**
     * Non-empty token used only to publish a cancel handle.
     * {@code PartialResponse} rejects an empty string.
     */
    static final String HOLD_TOKEN = ".";

    /** Summary text written by the blocking model. */
    public static final String SUMMARY = "folded history";

    /** Message count that triggers compaction in the UI suite. */
    public static final int THRESHOLD = 4;

    /** Recent messages kept by compaction in the UI suite. */
    public static final int KEEP = 2;

    private static final int POLL_MS = 20;

    private ScriptedChat() {
    }

    /**
     * Returns whether the UI suite selected this provider.
     *
     * @return {@code true} when {@link #PROPERTY} is set.
     */
    public static boolean isEnabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    /**
     * Returns the scripted provider.
     *
     * @return a provider with no network calls.
     */
    public static ModelProvider provider() {
        return new ModelProvider() {
            @Override
            public StreamingChatModel streaming() {
                return new StreamingChatModel() {
                    @Override
                    public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                        stream(request, handler);
                    }
                };
            }

            @Override
            public ChatModel blocking() {
                return new ChatModel() {
                    @Override
                    public ChatResponse doChat(final ChatRequest request) {
                        return summarize(request);
                    }
                };
            }
        };
    }

    /**
     * Streams {@link #REPLY}, or waits for cancel while {@link #HOLD} is set.
     *
     * @param request the chat request. The text is ignored.
     * @param handler the graph handler.
     */
    private static void stream(final ChatRequest request, final StreamingChatResponseHandler handler) {
        Thread thread = new Thread(() -> reply(handler), "zjoule-e2e-model");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Returns the fixed summary.
     *
     * @param request the summary request. The text is ignored.
     * @return the scripted summary.
     */
    private static ChatResponse summarize(final ChatRequest request) {
        return ChatResponse.builder().aiMessage(AiMessage.from(SUMMARY)).build();
    }

    /**
     * Emits the reply on the model thread.
     *
     * @param handler the graph handler.
     */
    private static void reply(final StreamingChatResponseHandler handler) {
        if (Boolean.getBoolean(HOLD)) {
            if (waitForCancel(handler)) {
                return;
            }
        }
        handler.onPartialResponse(REPLY);
        handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from(REPLY)).build());
    }

    /**
     * Publishes a cancel handle and waits until Stop or the hold flag drops.
     *
     * @param handler the graph handler.
     * @return {@code true} when the turn was cancelled.
     */
    private static boolean waitForCancel(final StreamingChatResponseHandler handler) {
        AtomicBoolean cancelled = new AtomicBoolean();
        StreamingHandle handle = new StreamingHandle() {
            @Override
            public void cancel() {
                cancelled.set(true);
            }

            @Override
            public boolean isCancelled() {
                return cancelled.get();
            }
        };
        handler.onPartialResponse(new PartialResponse(HOLD_TOKEN), new PartialResponseContext(handle));
        System.setProperty(ARMED, "true");
        while (Boolean.getBoolean(HOLD) && !cancelled.get()) {
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return true;
            }
        }
        return cancelled.get();
    }
}
