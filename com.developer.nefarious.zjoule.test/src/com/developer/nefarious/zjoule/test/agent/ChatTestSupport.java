package com.developer.nefarious.zjoule.test.agent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.developer.nefarious.zjoule.plugin.agent.ChatRuntime;
import com.developer.nefarious.zjoule.plugin.agent.ChatStreamListener;
import com.developer.nefarious.zjoule.plugin.agent.PromptAssembler;
import com.developer.nefarious.zjoule.plugin.agent.RunHandle;
import com.developer.nefarious.zjoule.plugin.agent.model.ModelProvider;
import com.developer.nefarious.zjoule.plugin.agent.persistence.ActiveSessionStore;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.StreamingHandle;

/**
 * Shared stand-ins for chat behavior tests. No network.
 */
public final class ChatTestSupport {

    /** How long a test waits for a background job. */
    public static final int WAIT_SECONDS = 10;

    /** Pause between polls of {@link RunHandle#isDone()}. */
    private static final int POLL_MS = 10;

    /** Threshold used by tests that are not exercising compaction. */
    public static final int NO_COMPACTION = Integer.MAX_VALUE;

    /** Keep count paired with {@link #NO_COMPACTION}. */
    public static final int TEST_KEEP = 20;

    /** Provider id written into a fresh index. */
    public static final String PROVIDER = "sap";

    /** Model name written into a fresh index. */
    public static final String MODEL = "gpt-4o";

    /** Instruction text that must not be stored in the checkpoint. */
    public static final String INSTRUCTIONS = "INSTR_SENTINEL";

    private ChatTestSupport() {
    }

    /**
     * Opens a runtime on a directory with a scripted streaming model.
     *
     * @param directory the sessions directory.
     * @param model the streaming model.
     * @return the runtime.
     * @throws Exception when the graph cannot be compiled.
     */
    public static ChatRuntime runtime(final Path directory, final StreamingChatModel model) throws Exception {
        return runtime(directory, provider(model), NO_COMPACTION, TEST_KEEP);
    }

    /**
     * Opens a runtime with an explicit compaction limit.
     *
     * @param directory the sessions directory.
     * @param models the model provider.
     * @param threshold the message count that triggers compaction.
     * @param keep how many recent messages to retain.
     * @return the runtime.
     * @throws Exception when the graph cannot be compiled.
     */
    public static ChatRuntime runtime(final Path directory, final ModelProvider models, final int threshold,
            final int keep) throws Exception {
        PromptAssembler prompt = new PromptAssembler(() -> INSTRUCTIONS);
        return new ChatRuntime(directory, models, prompt, new MemoryActive(), () -> PROVIDER, () -> MODEL,
                Clock.systemUTC(), () -> threshold, () -> keep);
    }

    /**
     * Sends one turn and waits until the job finishes successfully.
     *
     * @param runtime the runtime.
     * @param threadId the thread id.
     * @param text the user text.
     * @param editorContext the editor text, or {@code null}.
     * @return the recording.
     * @throws InterruptedException when the wait is interrupted.
     */
    public static Recording send(final ChatRuntime runtime, final String threadId, final String text,
            final String editorContext) throws InterruptedException {
        Recording recording = new Recording();
        RunHandle handle = runtime.send(threadId, text, editorContext, recording);
        await(recording.finished);
        awaitDone(handle);
        if (recording.error != null) {
            throw new AssertionError(recording.error);
        }
        assertFalse(recording.cancelled);
        return recording;
    }

    /**
     * Waits until the latch opens.
     *
     * @param latch the latch.
     * @throws InterruptedException when the wait is interrupted.
     */
    public static void await(final CountDownLatch latch) throws InterruptedException {
        assertTrue(latch.await(WAIT_SECONDS, TimeUnit.SECONDS), "timed out");
    }

    /**
     * Waits until the job marks itself done.
     *
     * @param handle the run.
     * @throws InterruptedException when the wait is interrupted.
     */
    public static void awaitDone(final RunHandle handle) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (!handle.isDone()) {
            if (System.nanoTime() > deadline) {
                fail("chat job did not finish");
            }
            Thread.sleep(POLL_MS);
        }
    }

    /**
     * Returns the message types stored for a thread.
     *
     * @param runtime the runtime.
     * @param threadId the thread id.
     * @return the types, oldest first.
     */
    public static List<ChatMessageType> types(final ChatRuntime runtime, final String threadId) {
        List<ChatMessageType> types = new ArrayList<>();
        for (ChatMessage message : runtime.history(threadId)) {
            types.add(message.type());
        }
        return types;
    }

    /**
     * A model that replies with a fixed string on a daemon thread.
     *
     * @param text the assistant text.
     * @return the model.
     */
    public static StreamingChatModel replying(final String text) {
        return new StreamingChatModel() {
            @Override
            public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                Thread thread = new Thread(() -> complete(handler, text), "zjoule-test-model");
                thread.setDaemon(true);
                thread.start();
            }
        };
    }

    /**
     * A model that emits one token, then waits. Cancel skips the completion callback.
     *
     * @param tokenSeen opened when the token callback has been invoked.
     * @param release opened by {@link StreamingHandle#cancel()}.
     * @param completed set when the model still calls completion.
     * @return the model.
     */
    public static StreamingChatModel cancellable(final CountDownLatch tokenSeen, final CountDownLatch release,
            final AtomicBoolean completed) {
        AtomicInteger calls = new AtomicInteger();
        return new StreamingChatModel() {
            @Override
            public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                if (calls.incrementAndGet() > 1) {
                    Thread later = new Thread(() -> complete(handler, "ok"), "zjoule-test-later");
                    later.setDaemon(true);
                    later.start();
                    return;
                }
                Thread thread = new Thread(() -> emitOrStop(handler, tokenSeen, release, completed),
                        "zjoule-test-cancel");
                thread.setDaemon(true);
                thread.start();
            }
        };
    }

    /**
     * Wraps a streaming model as the login-independent provider.
     *
     * @param model the streaming model.
     * @return the provider.
     */
    public static ModelProvider provider(final StreamingChatModel model) {
        return new ModelProvider() {
            @Override
            public StreamingChatModel streaming() {
                return model;
            }

            @Override
            public ChatModel blocking() {
                throw new UnsupportedOperationException("blocking model is not configured");
            }
        };
    }

    /**
     * Completes a reply.
     *
     * @param handler the handler.
     * @param text the assistant text.
     */
    private static void complete(final StreamingChatResponseHandler handler, final String text) {
        handler.onPartialResponse(text);
        handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from(text)).build());
    }

    /**
     * Emits one token and completes only when the caller did not cancel.
     *
     * @param handler the handler.
     * @param tokenSeen opened after the token is offered.
     * @param release opened by cancel.
     * @param completed set when completion still runs.
     */
    private static void emitOrStop(final StreamingChatResponseHandler handler, final CountDownLatch tokenSeen,
            final CountDownLatch release, final AtomicBoolean completed) {
        StreamingHandle handle = new StreamingHandle() {
            @Override
            public void cancel() {
                release.countDown();
            }

            @Override
            public boolean isCancelled() {
                return release.getCount() == 0;
            }
        };
        handler.onPartialResponse(new PartialResponse("x"), new PartialResponseContext(handle));
        tokenSeen.countDown();
        try {
            release.await(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        if (handle.isCancelled()) {
            return;
        }
        completed.set(true);
        handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("x")).build());
    }

    /**
     * Records listener callbacks.
     */
    public static final class Recording implements ChatStreamListener {

        /** Opened when the turn reaches a terminal callback. */
        public final CountDownLatch finished = new CountDownLatch(1);

        /** Tokens in arrival order. */
        public final List<String> tokens = new ArrayList<>();

        /** Set on {@link #onComplete(String)}. */
        public volatile String fullText;

        /** Set on {@link #onCancelled()}. */
        public volatile boolean cancelled;

        /** Set on {@link #onError(Throwable)}. */
        public volatile Throwable error;

        @Override
        public void onStart() {
        }

        @Override
        public void onToken(final String chunk) {
            tokens.add(chunk);
        }

        @Override
        public void onComplete(final String text) {
            this.fullText = text;
            finished.countDown();
        }

        @Override
        public void onApprovalRequired(final String toolSummaryJson) {
            finished.countDown();
        }

        @Override
        public void onCancelled() {
            cancelled = true;
            finished.countDown();
        }

        @Override
        public void onError(final Throwable failure) {
            this.error = failure;
            finished.countDown();
        }
    }

    /**
     * In-memory active session id.
     */
    public static final class MemoryActive implements ActiveSessionStore {

        private String threadId;

        @Override
        public String get() {
            return threadId;
        }

        @Override
        public void set(final String id) {
            this.threadId = id;
        }
    }
}
