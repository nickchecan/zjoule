package com.developer.nefarious.zjoule.plugin.agent.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.InterruptionMetadata;
import org.bsc.langgraph4j.checkpoint.AbstractCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver.Tag;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.bsc.langgraph4j.serializer.CheckpointListSerializer;
import org.bsc.langgraph4j.serializer.Serializer;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.bsc.langgraph4j.state.AgentState;

import com.developer.nefarious.zjoule.plugin.agent.ChatState;

/**
 * Stores the newest checkpoint for each chat thread as one JSON file.
 */
public final class StateLocationCheckpointSaver extends AbstractCheckpointSaver {

    /** Index file that is not a thread checkpoint. */
    public static final String INDEX_FILE_NAME = "index.json";

    /** Suffix of a committed session file. */
    public static final String FILE_SUFFIX = ".json";

    /** Suffix of a file that is still being written. */
    public static final String TEMP_SUFFIX = ".json.tmp";

    /** Thread id that would collide with {@link #INDEX_FILE_NAME}. */
    private static final String INDEX_STEM = "index";

    /** Prefix of the exception used when a session file cannot be parsed. */
    static final String CORRUPT_PREFIX = "Corrupt session file for thread ";

    /** How many stack frames to scan for {@code AbstractCheckpointSaver.put}. */
    private static final int PUT_FRAME_LIMIT = 8;

    private final Path directory;

    private final Serializer<LinkedList<Checkpoint>> serializer;

    private final int retain;

    /**
     * Creates the saver and the session directory.
     *
     * @param directory the sessions directory.
     * @param serializer the graph state serializer.
     * @param retain how many newest checkpoints to keep. v1 uses 1.
     * @throws IOException when the directory cannot be created.
     */
    public StateLocationCheckpointSaver(final Path directory, final StateSerializer<ChatState> serializer,
            final int retain) throws IOException {
        if (retain < 1) {
            throw new IllegalArgumentException("retain must be at least 1");
        }
        this.directory = directory;
        this.serializer = CheckpointListSerializer.of(serializer);
        this.retain = retain;
        Files.createDirectories(directory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LinkedList<Checkpoint> loadCheckpoints(final RunnableConfig config) throws Exception {
        String threadId = threadId(config);
        Path file = sessionFile(threadId);
        if (!Files.exists(file)) {
            return new LinkedList<>();
        }
        if (loadedForPut()) {
            return readReplacingCorrupt(file, threadId);
        }
        return read(file, threadId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void insertedCheckpoint(final RunnableConfig config, final LinkedList<Checkpoint> checkpoints,
            final Checkpoint checkpoint) throws Exception {
        trim(checkpoints);
        AtomicFiles.write(sessionFile(threadId(config)), serializer.writeDataAsString(checkpoints));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updatedCheckpoint(final RunnableConfig config, final LinkedList<Checkpoint> checkpoints,
            final Checkpoint checkpoint) throws Exception {
        insertedCheckpoint(config, checkpoints, checkpoint);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Releasing a thread must not delete or rename the session file.
     */
    @Override
    protected Tag releaseCheckpoints(final RunnableConfig config, final LinkedList<Checkpoint> checkpoints,
            final String message) {
        return new Tag(threadId(config), copyOf(checkpoints));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Tag releaseCheckpointsOnError(final RunnableConfig config, final LinkedList<Checkpoint> checkpoints,
            final Throwable error) {
        return new Tag(threadId(config), copyOf(checkpoints));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Tag> tag(final RunnableConfig config, final Integer version) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends AgentState> CompletableFuture<InterruptionMetadata<S>> registerInterruption(
            final RunnableConfig config, final InterruptionMetadata<S> metadata) {
        return CompletableFuture.completedFuture(metadata);
    }

    /**
     * Deletes the session file for a thread.
     *
     * @param threadId the thread id.
     * @return {@code true} when a file was removed.
     * @throws IOException when the delete fails.
     */
    public boolean delete(final String threadId) throws IOException {
        return Files.deleteIfExists(sessionFile(threadId));
    }

    /**
     * Returns the thread ids that have a committed session file.
     *
     * @return the ids, in sorted order.
     * @throws IOException when the directory cannot be listed.
     */
    public Set<String> threadIds() throws IOException {
        if (!Files.isDirectory(directory)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(StateLocationCheckpointSaver::isSessionFile)
                    .map(name -> name.substring(0, name.length() - FILE_SUFFIX.length()))
                    .collect(TreeSet::new, Set::add, Set::addAll);
        }
    }

    /**
     * Trims the list so only the newest checkpoints remain.
     * The newest checkpoint is at the head.
     *
     * @param checkpoints the checkpoints for one thread.
     */
    private void trim(final LinkedList<Checkpoint> checkpoints) {
        while (checkpoints.size() > retain) {
            checkpoints.removeLast();
        }
    }

    /**
     * Reads a session file for {@code put}. A corrupt file is an empty history so the
     * final {@code put} can replace it. {@code list} and {@code get} still fail and
     * leave the bytes in place.
     *
     * @param file the committed file.
     * @param threadId the thread id.
     * @return the checkpoints, or an empty list when the file is corrupt.
     * @throws IOException when the file cannot be read for a reason other than corruption.
     */
    private LinkedList<Checkpoint> readReplacingCorrupt(final Path file, final String threadId) throws IOException {
        try {
            return read(file, threadId);
        } catch (IOException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith(CORRUPT_PREFIX)) {
                return new LinkedList<>();
            }
            throw ex;
        }
    }

    /**
     * Returns whether this load is the one {@code AbstractCheckpointSaver.put} performs
     * before it writes. That method is final, so this is the only place a corrupt file
     * can be replaced.
     *
     * @return {@code true} when {@code put} is on the stack.
     */
    private static boolean loadedForPut() {
        return StackWalker.getInstance().walk(frames -> frames.limit(PUT_FRAME_LIMIT)
                .anyMatch(frame -> "put".equals(frame.getMethodName())
                        && "org.bsc.langgraph4j.checkpoint.AbstractCheckpointSaver".equals(frame.getClassName())));
    }

    /**
     * Reads a session file and refuses to delete it when the payload is bad.
     *
     * @param file the committed file.
     * @param threadId the thread id, included in the error.
     * @return the checkpoints.
     * @throws IOException when the file is unreadable.
     */
    private LinkedList<Checkpoint> read(final Path file, final String threadId) throws IOException {
        try {
            String payload = Files.readString(file);
            if (payload.isBlank()) {
                throw corrupt(threadId);
            }
            LinkedList<Checkpoint> loaded = serializer.readDataFromString(payload);
            if (loaded == null) {
                throw corrupt(threadId);
            }
            return loaded;
        } catch (IOException | ClassNotFoundException ex) {
            throw corrupt(threadId, ex);
        } catch (RuntimeException ex) {
            throw corrupt(threadId, ex);
        }
    }

    /**
     * Resolves a thread id to a file inside the sessions directory.
     *
     * @param threadId the thread id.
     * @return the committed session file.
     */
    private Path sessionFile(final String threadId) {
        if (!isSafeThreadId(threadId)) {
            throw new IllegalArgumentException("Invalid thread id");
        }
        Path file = directory.resolve(threadId + FILE_SUFFIX).normalize();
        Path parent = directory.normalize();
        if (file.getParent() == null || !file.getParent().equals(parent)) {
            throw new IllegalArgumentException("Invalid thread id");
        }
        return file;
    }

    /**
     * Returns whether a thread id can be used as a single file name.
     *
     * @param threadId the candidate id.
     * @return {@code true} when the id is safe.
     */
    private static boolean isSafeThreadId(final String threadId) {
        if (threadId == null || threadId.isBlank() || INDEX_STEM.equals(threadId)) {
            return false;
        }
        return threadId.indexOf('/') < 0 && threadId.indexOf('\\') < 0 && !threadId.contains("..");
    }

    /**
     * Returns whether a file name is a committed session file.
     *
     * @param name the file name.
     * @return {@code true} for {@code {threadId}.json} only.
     */
    private static boolean isSessionFile(final String name) {
        return name.endsWith(FILE_SUFFIX) && !name.endsWith(TEMP_SUFFIX) && !INDEX_FILE_NAME.equals(name);
    }

    /**
     * Copies checkpoints for a tag without exposing the live list.
     *
     * @param checkpoints the checkpoints.
     * @return an immutable copy.
     */
    private static Collection<Checkpoint> copyOf(final LinkedList<Checkpoint> checkpoints) {
        return List.copyOf(checkpoints);
    }

    /**
     * Builds the corrupt-file error.
     *
     * @param threadId the thread id.
     * @return the error.
     */
    private static IOException corrupt(final String threadId) {
        return new IOException(CORRUPT_PREFIX + threadId);
    }

    /**
     * Builds the corrupt-file error with the parser failure attached.
     *
     * @param threadId the thread id.
     * @param cause the parser failure.
     * @return the error.
     */
    private static IOException corrupt(final String threadId, final Exception cause) {
        if (cause instanceof IOException io && io.getMessage() != null && io.getMessage().startsWith(CORRUPT_PREFIX)) {
            return io;
        }
        return new IOException(CORRUPT_PREFIX + threadId, cause);
    }
}
