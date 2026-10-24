package com.developer.nefarious.zjoule.test.agent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.junit.jupiter.api.Test;

import com.developer.nefarious.zjoule.plugin.agent.ChatState;
import com.developer.nefarious.zjoule.plugin.agent.persistence.StateLocationCheckpointSaver;

import dev.langchain4j.data.message.UserMessage;

public class StateLocationCheckpointSaverTest {

    private static final String THREAD = "thread-1";

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    public void shouldRoundTripTheNewestCheckpoint() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-saver");
        StateSerializer<ChatState> serializer = ChatState.serializer();
        StateLocationCheckpointSaver saver = new StateLocationCheckpointSaver(directory, serializer, 1);
        RunnableConfig config = config(THREAD);

        saver.put(config, checkpoint("one", "one"));
        saver.put(config, checkpoint("two", "two"));
        saver.put(config, checkpoint("three", "three"));

        var loaded = saver.list(config);
        assertEquals(1, loaded.size());
        ChatState state = serializer.stateOf(loaded.iterator().next().state());
        assertEquals("three", ((UserMessage) state.messages().get(0)).singleText());
        assertEquals("three", saver.get(config).orElseThrow().id());
        assertTrue(saver.threadIds().contains(THREAD));
        assertFalse(Files.exists(directory.resolve(THREAD + ".json.tmp")));
    }

    @Test
    public void shouldIgnoreAPartialTempFileAndKeepTheCommittedBytes() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-saver");
        StateSerializer<ChatState> serializer = ChatState.serializer();
        StateLocationCheckpointSaver saver = new StateLocationCheckpointSaver(directory, serializer, 1);
        RunnableConfig config = config(THREAD);
        saver.put(config, checkpoint("kept", "kept"));
        Files.writeString(directory.resolve(THREAD + ".json.tmp"), "partial write");

        assertEquals("kept", ((UserMessage) serializer.stateOf(saver.get(config).orElseThrow().state()).messages()
                .get(0)).singleText());
        assertFalse(saver.threadIds().contains("index"));
        assertTrue(saver.threadIds().contains(THREAD));

        Path onlyTemp = directory.resolve("orphan.json.tmp");
        Files.writeString(onlyTemp, "not a session");
        assertFalse(saver.threadIds().contains("orphan"));
        assertTrue(saver.get(config("orphan")).isEmpty());
    }

    @Test
    public void shouldLeaveACorruptFileInPlaceUntilALaterWriteReplacesIt() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-saver");
        StateSerializer<ChatState> serializer = ChatState.serializer();
        StateLocationCheckpointSaver saver = new StateLocationCheckpointSaver(directory, serializer, 1);
        Path file = directory.resolve(THREAD + ".json");
        byte[] corrupt = "{not json".getBytes();
        Files.write(file, corrupt);
        RunnableConfig config = config(THREAD);

        RuntimeException failure = assertThrows(RuntimeException.class, () -> saver.get(config));
        assertTrue(failure.getCause().getMessage().contains("Corrupt session file for thread " + THREAD));
        assertArrayEquals(corrupt, Files.readAllBytes(file));
        assertThrows(RuntimeException.class, () -> saver.list(config));
        assertArrayEquals(corrupt, Files.readAllBytes(file));

        saver.put(config, checkpoint("recovered", "recovered"));
        assertEquals("recovered", ((UserMessage) serializer.stateOf(saver.get(config).orElseThrow().state())
                .messages().get(0)).singleText());
    }

    @Test
    public void shouldRejectUnsafeThreadIdsAndKeepTheFileOnRelease() throws Exception {
        Path directory = Files.createTempDirectory("zjoule-saver");
        StateSerializer<ChatState> serializer = ChatState.serializer();
        StateLocationCheckpointSaver saver = new StateLocationCheckpointSaver(directory, serializer, 1);
        assertThrows(IllegalArgumentException.class, () -> saver.delete("index"));
        assertThrows(IllegalArgumentException.class, () -> saver.delete("../x"));
        assertThrows(IllegalArgumentException.class, () -> saver.delete("a/b"));

        RunnableConfig config = config(THREAD);
        saver.put(config, checkpoint("stay", "stay"));
        saver.release(config);

        assertTrue(Files.exists(directory.resolve(THREAD + ".json")));
        assertEquals(1, saver.list(config).size());
        Files.writeString(directory.resolve("index.json"), "[]");
        assertFalse(saver.threadIds().contains("index"));
    }

    private static RunnableConfig config(final String threadId) {
        return RunnableConfig.builder().threadId(threadId).build();
    }

    private static Checkpoint checkpoint(final String id, final String text) {
        Map<String, Object> state = new HashMap<>();
        state.put(ChatState.MESSAGES, java.util.List.of(UserMessage.from(text)));
        state.put(ChatState.SUMMARY, "");
        state.put(ChatState.SUMMARIZED_COUNT, 0);
        state.put(ChatState.APPROVAL, "");
        return Checkpoint.builder().id(id).state(state).nodeId("call_model").nextNodeId("END").createdAt(CREATED)
                .build();
    }
}
