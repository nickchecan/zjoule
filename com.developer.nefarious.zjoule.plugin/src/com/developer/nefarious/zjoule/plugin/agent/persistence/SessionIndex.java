package com.developer.nefarious.zjoule.plugin.agent.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.bsc.langgraph4j.serializer.StateSerializer;

import com.developer.nefarious.zjoule.plugin.agent.ChatState;
import com.google.gson.Gson;

import dev.langchain4j.data.message.ChatMessage;

/**
 * Cache of session metadata. The checkpoint files remain the source of history.
 */
public final class SessionIndex {

    private final Path file;

    private final Path directory;

    private final StateLocationCheckpointSaver saver;

    private final StateSerializer<ChatState> serializer;

    private final Gson gson = new Gson();

    /**
     * Creates the index next to the session files.
     *
     * @param directory the sessions directory.
     * @param saver the checkpoint saver.
     * @param serializer the state serializer used to read titles.
     */
    public SessionIndex(final Path directory, final StateLocationCheckpointSaver saver,
            final StateSerializer<ChatState> serializer) {
        this.directory = directory;
        this.file = directory.resolve(StateLocationCheckpointSaver.INDEX_FILE_NAME);
        this.saver = saver;
        this.serializer = serializer;
    }

    /**
     * Returns the cached sessions, rebuilding the file when it is missing or corrupt.
     *
     * @return the sessions.
     */
    public synchronized List<SessionInfo> read() {
        if (!Files.exists(file)) {
            return rebuild();
        }
        try {
            SessionInfo[] items = gson.fromJson(Files.readString(file), SessionInfo[].class);
            if (items == null) {
                return rebuild();
            }
            return new ArrayList<>(Arrays.asList(items));
        } catch (IOException | RuntimeException ex) {
            return rebuild();
        }
    }

    /**
     * Adds or replaces one session and writes the index.
     *
     * @param session the session.
     */
    public synchronized void save(final SessionInfo session) {
        List<SessionInfo> sessions = read();
        sessions.removeIf(item -> session.threadId().equals(item.threadId()));
        sessions.add(session);
        persist(sessions);
    }

    /**
     * Removes one session from the index.
     *
     * @param threadId the thread id.
     */
    public synchronized void remove(final String threadId) {
        List<SessionInfo> sessions = read();
        sessions.removeIf(item -> threadId.equals(item.threadId()));
        persist(sessions);
    }

    /**
     * Updates the title and timestamps after a turn.
     *
     * @param threadId the thread id.
     * @param provider the provider id.
     * @param model the model name.
     * @param messages the messages now stored in the checkpoint.
     * @param updatedAt the update time in epoch milliseconds.
     */
    public synchronized void touch(final String threadId, final String provider, final String model,
            final List<ChatMessage> messages, final long updatedAt) {
        List<SessionInfo> sessions = read();
        SessionInfo current = find(sessions, threadId);
        long created = current == null ? updatedAt : current.createdAtMs();
        String title = current == null ? SessionTitles.PLACEHOLDER : current.title();
        if (SessionTitles.replaceable(title)) {
            title = SessionTitles.fromMessages(messages);
        }
        save(new SessionInfo(threadId, title, created, updatedAt, blank(provider), blank(model)));
    }

    /**
     * Rebuilds the index by scanning committed session files.
     *
     * @return the rebuilt sessions.
     */
    private List<SessionInfo> rebuild() {
        List<SessionInfo> rebuilt = new ArrayList<>();
        try {
            for (String threadId : saver.threadIds()) {
                rebuilt.add(describe(threadId));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not scan sessions", ex);
        }
        persist(rebuilt);
        return new ArrayList<>(rebuilt);
    }

    /**
     * Describes one thread from its checkpoint file.
     *
     * @param threadId the thread id.
     * @return the session metadata. Provider and model are blank.
     */
    private SessionInfo describe(final String threadId) {
        long created = createdAt(threadId);
        try {
            Collection<Checkpoint> checkpoints = saver.list(RunnableConfig.builder().threadId(threadId).build());
            if (checkpoints.isEmpty()) {
                return new SessionInfo(threadId, SessionTitles.PLACEHOLDER, created, created, "", "");
            }
            Checkpoint newest = checkpoints.iterator().next();
            return fromCheckpoint(threadId, created, newest);
        } catch (RuntimeException ex) {
            return new SessionInfo(threadId, SessionTitles.UNREADABLE, created, created, "", "");
        }
    }

    /**
     * Builds metadata from the newest checkpoint.
     *
     * @param threadId the thread id.
     * @param created the file creation time.
     * @param newest the newest checkpoint.
     * @return the session metadata.
     */
    private SessionInfo fromCheckpoint(final String threadId, final long created, final Checkpoint newest) {
        ChatState state = serializer.stateOf(newest.state());
        long updated = newest.createdAt() == null ? created : newest.createdAt().toEpochMilli();
        String title = SessionTitles.fromMessages(state.messages());
        return new SessionInfo(threadId, title, created, updated, "", "");
    }

    /**
     * Reads the file creation time, falling back to the last modified time.
     *
     * @param threadId the thread id.
     * @return epoch milliseconds, or 0 when the file cannot be read.
     */
    private long createdAt(final String threadId) {
        Path session = directory.resolve(threadId + StateLocationCheckpointSaver.FILE_SUFFIX);
        try {
            FileTime created = (FileTime) Files.getAttribute(session, "creationTime");
            return created.toMillis();
        } catch (IOException ex) {
            return lastModified(session);
        }
    }

    /**
     * Returns the last modified time, or 0.
     *
     * @param session the session file.
     * @return epoch milliseconds.
     */
    private long lastModified(final Path session) {
        try {
            return Files.getLastModifiedTime(session).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    /**
     * Writes the index.
     *
     * @param sessions the sessions.
     */
    private void persist(final List<SessionInfo> sessions) {
        try {
            AtomicFiles.write(file, gson.toJson(sessions));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write the session index", ex);
        }
    }

    /**
     * Finds a session in a list.
     *
     * @param sessions the sessions.
     * @param threadId the thread id.
     * @return the match, or {@code null}.
     */
    private static SessionInfo find(final List<SessionInfo> sessions, final String threadId) {
        for (SessionInfo session : sessions) {
            if (threadId.equals(session.threadId())) {
                return session;
            }
        }
        return null;
    }

    /**
     * Replaces a null provider or model with an empty string.
     *
     * @param value the value.
     * @return a non-null string.
     */
    private static String blank(final String value) {
        return value == null ? "" : value;
    }
}
