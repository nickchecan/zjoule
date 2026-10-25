package com.developer.nefarious.zjoule.test.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.developer.nefarious.zjoule.plugin.agent.ChatRuntime;
import com.developer.nefarious.zjoule.plugin.agent.HistoryCompaction;
import com.developer.nefarious.zjoule.plugin.agent.RunHandle;
import com.developer.nefarious.zjoule.plugin.agent.model.ModelProvider;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

public class HistoryCompactionTest {

    private static final int THRESHOLD = 4;

    private static final int KEEP = 2;

    private static final int TOOL_THRESHOLD = 3;

    private static final int KEEP_ABOVE_THRESHOLD = 100;

    private static final String SUMMARY = "folded history";

    private static final String OLD_ONE = "old-one";

    private static final String OLD_TWO = "old-two";

    private static final String FRESH = "fresh";

    @Test
    public void shouldLeaveHistoryUnderTheThreshold() {
        List<ChatMessage> messages = List.of(UserMessage.from("a"), AiMessage.from("b"), UserMessage.from("c"),
                AiMessage.from("d"));

        assertEquals(HistoryCompaction.NO_CUT, HistoryCompaction.keepFrom(messages, THRESHOLD, KEEP));
    }

    @Test
    public void shouldSummarizeThePrefixAndKeepTheTail() {
        List<ChatMessage> messages = List.of(UserMessage.from(OLD_ONE), AiMessage.from("ok"), UserMessage.from(OLD_TWO),
                AiMessage.from("ok"), UserMessage.from(FRESH));

        assertEquals(messages.size() - KEEP, HistoryCompaction.keepFrom(messages, THRESHOLD, KEEP));
    }

    @Test
    public void shouldKeepAToolRequestWithItsResult() {
        ToolExecutionRequest request = ToolExecutionRequest.builder().id("1").name("lookup").arguments("{}").build();
        List<ChatMessage> messages = List.of(UserMessage.from(OLD_ONE), AiMessage.from(request),
                ToolExecutionResultMessage.from(request, "done"), UserMessage.from(FRESH));

        assertEquals(1, HistoryCompaction.keepFrom(messages, TOOL_THRESHOLD, KEEP));
    }

    @Test
    public void shouldSkipCompactionWhenTheToolPairLeavesNothingToSummarize() {
        ToolExecutionRequest request = ToolExecutionRequest.builder().id("1").name("lookup").arguments("{}").build();
        List<ChatMessage> messages = List.of(AiMessage.from(request),
                ToolExecutionResultMessage.from(request, "done"));

        assertEquals(HistoryCompaction.NO_CUT, HistoryCompaction.keepFrom(messages, 1, 1));
    }

    @Test
    public void shouldShrinkWhenKeepIsNotBelowTheThreshold() {
        List<ChatMessage> messages = List.of(UserMessage.from("1"), AiMessage.from("2"), UserMessage.from("3"),
                AiMessage.from("4"), UserMessage.from("5"), AiMessage.from("6"));

        assertEquals(messages.size() - (THRESHOLD / 2),
                HistoryCompaction.keepFrom(messages, THRESHOLD, KEEP_ABOVE_THRESHOLD));
    }

    @Test
    public void shouldStoreTheSummaryAndDropThePrefixFromTheCheckpoint() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-compact");
        AtomicReference<ChatRequest> summaryRequest = new AtomicReference<>();
        AtomicReference<ChatRequest> lastPrompt = new AtomicReference<>();
        ChatRuntime runtime = ChatTestSupport.runtime(directory, folding(summaryRequest, lastPrompt, null, null),
                THRESHOLD, KEEP);
        String threadId = runtime.newSession().threadId();

        ChatTestSupport.send(runtime, threadId, OLD_ONE, null);
        ChatTestSupport.send(runtime, threadId, OLD_TWO, null);
        ChatTestSupport.send(runtime, threadId, FRESH, null);

        String checkpoint = Files.readString(directory.resolve(threadId + ".json"));
        assertTrue(checkpoint.contains(SUMMARY));
        assertFalse(checkpoint.contains(OLD_ONE));
        assertFalse(checkpoint.contains(OLD_TWO));
        assertTrue(checkpoint.contains(FRESH));
        assertEquals(List.of(ChatMessageType.AI, ChatMessageType.USER, ChatMessageType.AI),
                ChatTestSupport.types(runtime, threadId));
        assertTrue(summaryRequest.get().messages().get(0).toString().contains(OLD_ONE));
        assertFalse(summaryRequest.get().messages().get(0).toString().contains(FRESH));
        assertTrue(lastPrompt.get().messages().get(0).toString().contains(SUMMARY));
        assertTrue(lastPrompt.get().messages().toString().contains(FRESH));
        assertFalse(lastPrompt.get().messages().toString().contains(OLD_ONE));
    }

    @Test
    public void shouldKeepHistoryWhenCancelledDuringCompaction() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-compact-cancel");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger streams = new AtomicInteger();
        ChatRuntime runtime = ChatTestSupport.runtime(directory, folding(null, null, entered, release, streams),
                THRESHOLD, KEEP);
        String threadId = runtime.newSession().threadId();
        ChatTestSupport.send(runtime, threadId, OLD_ONE, null);
        ChatTestSupport.send(runtime, threadId, OLD_TWO, null);

        ChatTestSupport.Recording recording = new ChatTestSupport.Recording();
        RunHandle handle = runtime.send(threadId, FRESH, null, recording);
        assertTrue(entered.await(ChatTestSupport.WAIT_SECONDS, TimeUnit.SECONDS));
        handle.cancel();
        release.countDown();
        ChatTestSupport.await(recording.finished);
        ChatTestSupport.awaitDone(handle);

        String checkpoint = Files.readString(directory.resolve(threadId + ".json"));
        assertTrue(recording.cancelled);
        assertTrue(checkpoint.contains(OLD_ONE));
        assertTrue(checkpoint.contains(FRESH));
        assertFalse(checkpoint.contains(SUMMARY));
        assertEquals(2, streams.get());
    }

    private static ModelProvider folding(final AtomicReference<ChatRequest> summaryRequest,
            final AtomicReference<ChatRequest> lastPrompt, final CountDownLatch entered, final CountDownLatch release) {
        return folding(summaryRequest, lastPrompt, entered, release, new AtomicInteger());
    }

    private static ModelProvider folding(final AtomicReference<ChatRequest> summaryRequest,
            final AtomicReference<ChatRequest> lastPrompt, final CountDownLatch entered, final CountDownLatch release,
            final AtomicInteger streams) {
        return new ModelProvider() {
            @Override
            public StreamingChatModel streaming() {
                return new StreamingChatModel() {
                    @Override
                    public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                        streams.incrementAndGet();
                        if (lastPrompt != null) {
                            lastPrompt.set(request);
                        }
                        Thread thread = new Thread(() -> {
                            handler.onPartialResponse("ok");
                            handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("ok")).build());
                        }, "zjoule-test-fold-stream");
                        thread.setDaemon(true);
                        thread.start();
                    }
                };
            }

            @Override
            public ChatModel blocking() {
                return new ChatModel() {
                    @Override
                    public ChatResponse doChat(final ChatRequest request) {
                        if (summaryRequest != null) {
                            summaryRequest.set(request);
                        }
                        waitForRelease(entered, release);
                        return ChatResponse.builder().aiMessage(AiMessage.from(SUMMARY)).build();
                    }
                };
            }
        };
    }

    private static void waitForRelease(final CountDownLatch entered, final CountDownLatch release) {
        if (entered == null) {
            return;
        }
        entered.countDown();
        try {
            assertTrue(release.await(ChatTestSupport.WAIT_SECONDS, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
