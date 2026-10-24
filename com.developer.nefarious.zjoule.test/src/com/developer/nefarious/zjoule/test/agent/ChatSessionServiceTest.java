package com.developer.nefarious.zjoule.test.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.developer.nefarious.zjoule.plugin.agent.ChatRuntime;
import com.developer.nefarious.zjoule.plugin.agent.PromptAssembler;
import com.developer.nefarious.zjoule.plugin.agent.RunHandle;
import com.developer.nefarious.zjoule.plugin.agent.model.ModelProvider;
import com.developer.nefarious.zjoule.plugin.agent.model.RetryingStreamingChatModel;
import com.developer.nefarious.zjoule.plugin.agent.persistence.SessionInfo;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

public class ChatSessionServiceTest {

    private static final int TITLE_LIMIT = 60;

    private static final int LONG_TITLE = 80;

    private static final int DUPLICATE_TURNS = 3;

    private static final int FIRST_SIZE_TURNS = 10;

    private static final int TOTAL_SIZE_TURNS = 40;

    private static final int MESSAGE_CHARS = 200;

    private static final int MAX_SESSION_BYTES = 20_000;

    private static final int LINEAR_FACTOR = 6;

    private static final int UNAUTHORIZED = 401;

    private static final String EDITOR = "CLASS zcl_sentinel";

    @Test
    public void shouldCreateListAndDeleteSessions() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-sessions");
        ChatRuntime runtime = ChatTestSupport.runtime(directory, ChatTestSupport.replying("ok"));
        SessionInfo created = runtime.newSession();

        assertEquals(SessionInfo.NEW_CHAT_TITLE, created.title());
        assertEquals(created.threadId(), runtime.active().orElseThrow().threadId());
        assertEquals(1, runtime.list().size());

        ChatTestSupport.send(runtime, created.threadId(), "hello", null);
        runtime.delete(created.threadId());

        assertFalse(Files.exists(directory.resolve(created.threadId() + ".json")));
        assertTrue(runtime.list().stream().noneMatch(session -> created.threadId().equals(session.threadId())));
        assertNotEquals(created.threadId(), runtime.active().orElseThrow().threadId());
        assertEquals(SessionInfo.NEW_CHAT_TITLE, runtime.active().orElseThrow().title());
    }

    @Test
    public void shouldKeepDuplicatePromptsAndHistoryAcrossANewRuntime() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-sessions");
        StreamingChatModel model = ChatTestSupport.replying("ok");
        ChatRuntime runtime = ChatTestSupport.runtime(directory, model);
        String threadId = runtime.newSession().threadId();
        for (int turn = 0; turn < DUPLICATE_TURNS; turn++) {
            ChatTestSupport.send(runtime, threadId, "again", null);
        }

        assertEquals(List.of(ChatMessageType.USER, ChatMessageType.AI, ChatMessageType.USER, ChatMessageType.AI,
                ChatMessageType.USER, ChatMessageType.AI), ChatTestSupport.types(runtime, threadId));
        int users = 0;
        for (ChatMessage message : runtime.history(threadId)) {
            if (message instanceof UserMessage user && "again".equals(user.singleText())) {
                users++;
            }
        }
        assertEquals(DUPLICATE_TURNS, users);

        ChatRuntime restarted = ChatTestSupport.runtime(directory, model);
        assertEquals(DUPLICATE_TURNS * 2, restarted.history(threadId).size());
    }

    @Test
    public void shouldRebuildTheIndexFromCheckpointFiles() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-sessions");
        ChatRuntime runtime = ChatTestSupport.runtime(directory, ChatTestSupport.replying("ok"));
        String threadId = runtime.newSession().threadId();
        ChatTestSupport.send(runtime, threadId, "  hello   world  ", null);
        assertEquals("hello world", runtime.list().get(0).title());

        Files.writeString(directory.resolve("index.json"), "{{{{");
        ChatRuntime rebuilt = ChatTestSupport.runtime(directory, ChatTestSupport.replying("ok"));
        SessionInfo session = rebuilt.list().get(0);

        assertEquals(threadId, session.threadId());
        assertEquals("hello world", session.title());
        assertEquals("", session.provider());
        assertEquals("", session.model());
        assertTrue(Files.readString(directory.resolve("index.json")).contains(threadId));
    }

    @Test
    public void shouldDropEmptySessionsWhenTheIndexIsRebuilt() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-sessions");
        ChatRuntime runtime = ChatTestSupport.runtime(directory, ChatTestSupport.replying("ok"));
        runtime.newSession();
        Files.delete(directory.resolve("index.json"));

        assertTrue(ChatTestSupport.runtime(directory, ChatTestSupport.replying("ok")).list().isEmpty());
    }

    @Test
    public void shouldLimitTheTitleToSixtyCharacters() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-sessions");
        ChatRuntime runtime = ChatTestSupport.runtime(directory, ChatTestSupport.replying("ok"));
        String threadId = runtime.newSession().threadId();
        ChatTestSupport.send(runtime, threadId, "t".repeat(LONG_TITLE), null);

        assertEquals(TITLE_LIMIT, runtime.list().get(0).title().length());
    }

    @Test
    public void shouldBuildThePromptPerCallAndLeaveItOutOfTheCheckpoint() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-sessions");
        AtomicReference<ChatRequest> seen = new AtomicReference<>();
        ChatRuntime runtime = ChatTestSupport.runtime(directory, capturing(seen));
        String threadId = runtime.newSession().threadId();

        ChatTestSupport.send(runtime, threadId, "hello", EDITOR);

        ChatMessage system = seen.get().messages().get(0);
        assertEquals(ChatMessageType.SYSTEM, system.type());
        assertTrue(((SystemMessage) system).text().contains(ChatTestSupport.INSTRUCTIONS));
        assertTrue(((SystemMessage) system).text().contains(EDITOR));
        String stored = Files.readString(directory.resolve(threadId + ".json"));
        assertFalse(stored.contains(ChatTestSupport.INSTRUCTIONS));
        assertFalse(stored.contains(EDITOR));
        assertTrue(ChatTestSupport.types(runtime, threadId).stream().noneMatch(type -> type == ChatMessageType.SYSTEM));
    }

    @Test
    public void shouldKeepOnlyTheUserMessageWhenATurnIsCancelled() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-sessions");
        CountDownLatch tokenSeen = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);
        ChatRuntime runtime = ChatTestSupport.runtime(directory,
                ChatTestSupport.cancellable(tokenSeen, release, completed));
        String threadId = runtime.newSession().threadId();
        ChatTestSupport.Recording recording = new ChatTestSupport.Recording();

        RunHandle handle = runtime.send(threadId, "stop-me", null, recording);
        ChatTestSupport.await(tokenSeen);
        handle.cancel();
        ChatTestSupport.await(recording.finished);
        ChatTestSupport.awaitDone(handle);

        assertTrue(recording.cancelled);
        assertFalse(completed.get());
        assertEquals(List.of(ChatMessageType.USER), ChatTestSupport.types(runtime, threadId));

        ChatTestSupport.send(runtime, threadId, "next", null);
        assertEquals(List.of(ChatMessageType.USER, ChatMessageType.USER, ChatMessageType.AI),
                ChatTestSupport.types(runtime, threadId));
    }

    @Test
    public void shouldRejectASecondTurnUntilTheFirstFinishes() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-sessions");
        CountDownLatch tokenSeen = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ChatRuntime runtime = ChatTestSupport.runtime(directory,
                ChatTestSupport.cancellable(tokenSeen, release, new AtomicBoolean()));
        String threadId = runtime.newSession().threadId();
        ChatTestSupport.Recording recording = new ChatTestSupport.Recording();
        RunHandle first = runtime.send(threadId, "one", null, recording);
        ChatTestSupport.await(tokenSeen);

        assertThrows(IllegalStateException.class,
                () -> runtime.send(threadId, "two", null, new ChatTestSupport.Recording()));

        first.cancel();
        ChatTestSupport.awaitDone(first);
    }

    @Test
    public void shouldCancelARunningTurnWhenTheSessionIsDeleted() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-sessions");
        CountDownLatch tokenSeen = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ChatRuntime runtime = ChatTestSupport.runtime(directory,
                ChatTestSupport.cancellable(tokenSeen, release, new AtomicBoolean()));
        String threadId = runtime.newSession().threadId();
        runtime.send(threadId, "one", null, new ChatTestSupport.Recording());
        ChatTestSupport.await(tokenSeen);

        runtime.delete(threadId);

        assertFalse(Files.exists(directory.resolve(threadId + ".json")));
        assertTrue(runtime.list().stream().noneMatch(session -> threadId.equals(session.threadId())));
        assertNotEquals(threadId, runtime.active().orElseThrow().threadId());
    }

    @Test
    public void shouldRetryUnauthorizedOnceWithoutDuplicatingTheUserMessage() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-sessions");
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger invalidated = new AtomicInteger();
        ChatRuntime runtime = new ChatRuntime(directory, retrying(calls, invalidated),
                new PromptAssembler(() -> "instructions", () -> ChatTestSupport.WIDE_WINDOW),
                new ChatTestSupport.MemoryActive(), () -> ChatTestSupport.PROVIDER, () -> ChatTestSupport.MODEL,
                Clock.systemUTC());
        String threadId = runtime.newSession().threadId();

        ChatTestSupport.send(runtime, threadId, "hello", null);

        assertEquals(2, calls.get());
        assertEquals(1, invalidated.get());
        assertEquals(List.of(ChatMessageType.USER, ChatMessageType.AI), ChatTestSupport.types(runtime, threadId));
        assertEquals("ok", ((AiMessage) runtime.history(threadId).get(1)).text());
    }

    @Test
    public void shouldKeepTheSessionFileSmallAndLinear() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-sessions");
        ChatRuntime runtime = ChatTestSupport.runtime(directory, ChatTestSupport.replying("ok"));
        String threadId = runtime.newSession().threadId();
        String text = "m".repeat(MESSAGE_CHARS);
        for (int turn = 0; turn < FIRST_SIZE_TURNS; turn++) {
            ChatTestSupport.send(runtime, threadId, text, null);
        }
        long early = Files.size(directory.resolve(threadId + ".json"));
        for (int turn = FIRST_SIZE_TURNS; turn < TOTAL_SIZE_TURNS; turn++) {
            ChatTestSupport.send(runtime, threadId, text, null);
        }
        long later = Files.size(directory.resolve(threadId + ".json"));

        assertTrue(later < MAX_SESSION_BYTES);
        assertTrue(later < early * LINEAR_FACTOR);
        try (Stream<Path> files = Files.list(directory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    private static StreamingChatModel capturing(final AtomicReference<ChatRequest> seen) {
        return new StreamingChatModel() {
            @Override
            public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                seen.set(request);
                Thread thread = new Thread(() -> {
                    handler.onPartialResponse("ok");
                    handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("ok")).build());
                }, "zjoule-test-capture");
                thread.setDaemon(true);
                thread.start();
            }
        };
    }

    private static ModelProvider retrying(final AtomicInteger calls, final AtomicInteger invalidated) {
        StreamingChatModel fake = new StreamingChatModel() {
            @Override
            public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                if (calls.incrementAndGet() == 1) {
                    throw new HttpException(UNAUTHORIZED, "unauthorized");
                }
                Thread thread = new Thread(() -> {
                    handler.onPartialResponse("ok");
                    handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("ok")).build());
                }, "zjoule-test-retry");
                thread.setDaemon(true);
                thread.start();
            }
        };
        return new ModelProvider() {
            @Override
            public StreamingChatModel streaming() {
                return new RetryingStreamingChatModel(() -> fake, invalidated::incrementAndGet);
            }

            @Override
            public ChatModel blocking() {
                throw new UnsupportedOperationException("blocking model is not used in v1");
            }
        };
    }
}
