package com.developer.nefarious.zjoule.plugin.agent;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.eclipse.core.runtime.Platform;

import com.developer.nefarious.zjoule.plugin.agent.model.AiCoreTokenProvider;
import com.developer.nefarious.zjoule.plugin.agent.model.LoginModelProvider;
import com.developer.nefarious.zjoule.plugin.agent.model.ModelProvider;
import com.developer.nefarious.zjoule.plugin.agent.persistence.ActiveSessionStore;
import com.developer.nefarious.zjoule.plugin.agent.persistence.PreferenceActiveSessionStore;
import com.developer.nefarious.zjoule.plugin.agent.persistence.SessionIndex;
import com.developer.nefarious.zjoule.plugin.agent.persistence.SessionInfo;
import com.developer.nefarious.zjoule.plugin.agent.persistence.StateLocationCheckpointSaver;
import com.developer.nefarious.zjoule.plugin.auth.AuthClient;
import com.developer.nefarious.zjoule.plugin.auth.AuthClientHelper;
import com.developer.nefarious.zjoule.plugin.core.Activator;
import com.developer.nefarious.zjoule.plugin.core.preferences.HistoryWindow;
import com.developer.nefarious.zjoule.plugin.core.preferences.Instruction;
import com.developer.nefarious.zjoule.plugin.login.memory.TemporaryMemoryAccessToken;
import com.developer.nefarious.zjoule.plugin.memory.EclipseMemory;
import com.developer.nefarious.zjoule.plugin.memory.MemoryServiceKey;
import com.developer.nefarious.zjoule.plugin.memory.SecureStore;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.StreamingHandle;

/**
 * Checkpointer-backed chat sessions for one plug-in instance.
 */
public final class ChatRuntime implements ChatSessionService {

    /** Only the newest checkpoint is kept for each thread. */
    private static final int RETAIN_NEWEST = 1;

    /** Directory under the plug-in state location. */
    private static final String SESSIONS = "sessions";

    private final StateLocationCheckpointSaver saver;

    private final CompiledGraph<ChatState> graph;

    private final SessionIndex index;

    private final ActiveSessionStore activeStore;

    private final Supplier<String> providerId;

    private final Supplier<String> modelName;

    private final Clock clock;

    private final AtomicReference<StreamingHandle> streamingHandle = new AtomicReference<>();

    private final Object gate = new Object();

    private ChatRun activeRun;

    /**
     * Opens the runtime used by the workbench.
     *
     * @return the runtime.
     */
    public static ChatRuntime openDefault() {
        try {
            return openWorkbench();
        } catch (IOException | GraphStateException ex) {
            throw new IllegalStateException("Could not open chat sessions", ex);
        }
    }

    /**
     * Creates a runtime on an existing directory. Tests use this constructor.
     *
     * @param directory the sessions directory.
     * @param models the model provider.
     * @param prompt the prompt assembler.
     * @param activeStore the active session id.
     * @param providerId the provider id written into the index.
     * @param modelName the model name written into the index.
     * @param clock the clock used for index timestamps.
     * @throws IOException when the directory cannot be created.
     * @throws GraphStateException when the graph cannot be compiled.
     */
    public ChatRuntime(final Path directory, final ModelProvider models, final PromptAssembler prompt,
            final ActiveSessionStore activeStore, final Supplier<String> providerId, final Supplier<String> modelName,
            final Clock clock) throws IOException, GraphStateException {
        org.bsc.langgraph4j.serializer.StateSerializer<ChatState> serializer = ChatState.serializer();
        this.saver = new StateLocationCheckpointSaver(directory, serializer, RETAIN_NEWEST);
        this.graph = ChatGraphFactory.create(models, prompt, saver, streamingHandle::set);
        this.index = new SessionIndex(directory, saver, serializer);
        this.activeStore = activeStore;
        this.providerId = providerId;
        this.modelName = modelName;
        this.clock = clock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SessionInfo newSession() {
        long now = clock.millis();
        SessionInfo session = new SessionInfo(UUID.randomUUID().toString(), SessionInfo.NEW_CHAT_TITLE, now, now,
                providerId.get(), modelName.get());
        index.save(session);
        activeStore.set(session.threadId());
        return session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SessionInfo> list() {
        List<SessionInfo> sessions = new ArrayList<>(index.read());
        sessions.sort(Comparator.comparingLong(SessionInfo::updatedAtMs).reversed());
        return List.copyOf(sessions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SessionInfo> active() {
        String threadId = activeStore.get();
        if (threadId == null || threadId.isBlank()) {
            return Optional.empty();
        }
        return find(threadId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activate(final String threadId) {
        if (find(threadId).isEmpty()) {
            throw new IllegalArgumentException("Unknown session " + threadId);
        }
        activeStore.set(threadId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatMessage> history(final String threadId) {
        Optional<StateSnapshot<ChatState>> snapshot = graph.lastStateOf(config(threadId));
        if (snapshot.isEmpty()) {
            return List.of();
        }
        return List.copyOf(snapshot.get().state().messages());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String threadId) {
        cancelMatching(threadId);
        removeFile(threadId);
        index.remove(threadId);
        if (threadId.equals(activeStore.get())) {
            newSession();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RunHandle send(final String threadId, final String userText, final String editorContext,
            final ChatStreamListener listener) {
        return start(threadId, userText, editorContext, false, false, listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RunHandle resume(final String threadId, final boolean approved, final ChatStreamListener listener) {
        return start(threadId, "", null, true, approved, listener);
    }

    /**
     * Cancels the running turn, if any.
     */
    public void shutdown() {
        synchronized (gate) {
            if (activeRun != null && !activeRun.isDone()) {
                activeRun.cancel();
            }
        }
    }

    /**
     * Returns the compiled graph.
     *
     * @return the graph.
     */
    CompiledGraph<ChatState> graph() {
        return graph;
    }

    /**
     * Refreshes the index after a turn, including a failed turn whose user message was stored.
     *
     * @param threadId the thread id.
     */
    void touch(final String threadId) {
        index.touch(threadId, providerId.get(), modelName.get(), history(threadId), clock.millis());
    }

    /**
     * Returns the text of the latest assistant message.
     *
     * @param threadId the thread id.
     * @return the text, or an empty string.
     */
    String lastAiText(final String threadId) {
        List<ChatMessage> messages = history(threadId);
        for (int position = messages.size() - 1; position >= 0; position--) {
            ChatMessage message = messages.get(position);
            if (message instanceof AiMessage ai && ai.text() != null) {
                return ai.text();
            }
        }
        return "";
    }

    /**
     * Schedules a turn. A second turn is rejected until the first job finishes.
     *
     * @param threadId the thread id.
     * @param userText the user message.
     * @param editorContext the editor text.
     * @param resume whether this is a resume.
     * @param approved the resume decision.
     * @param listener the listener.
     * @return the run handle.
     */
    private RunHandle start(final String threadId, final String userText, final String editorContext,
            final boolean resume, final boolean approved, final ChatStreamListener listener) {
        synchronized (gate) {
            if (activeRun != null && !activeRun.isDone()) {
                throw new IllegalStateException("A chat response is already running");
            }
            streamingHandle.set(null);
            ChatRun run = new ChatRun(threadId, listener, streamingHandle);
            ChatJob job = new ChatJob(this, run, userText, editorContext, resume, approved);
            run.attach(job);
            activeRun = run;
            job.schedule();
            return run;
        }
    }

    /**
     * Cancels and joins a turn that is still running on this thread.
     *
     * @param threadId the thread id.
     */
    private void cancelMatching(final String threadId) {
        ChatRun run;
        synchronized (gate) {
            run = activeRun;
        }
        if (run == null || run.isDone() || !threadId.equals(run.threadId())) {
            return;
        }
        run.cancel();
        join(run);
    }

    /**
     * Waits for a run to finish.
     *
     * @param run the run.
     */
    private void join(final ChatRun run) {
        try {
            run.join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Deletes the checkpoint file.
     *
     * @param threadId the thread id.
     */
    private void removeFile(final String threadId) {
        try {
            saver.delete(threadId);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete session " + threadId, ex);
        }
    }

    /**
     * Finds a session in the index.
     *
     * @param threadId the thread id.
     * @return the session.
     */
    private Optional<SessionInfo> find(final String threadId) {
        for (SessionInfo session : list()) {
            if (threadId.equals(session.threadId())) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    /**
     * Builds a config that only carries the thread id.
     *
     * @param threadId the thread id.
     * @return the config.
     */
    private static RunnableConfig config(final String threadId) {
        return RunnableConfig.builder().threadId(threadId).build();
    }

    /**
     * Wires the workbench runtime.
     *
     * @return the runtime.
     * @throws IOException when the sessions directory cannot be created.
     * @throws GraphStateException when the graph cannot be compiled.
     */
    private static ChatRuntime openWorkbench() throws IOException, GraphStateException {
        Path directory = Platform.getStateLocation(Activator.getDefault().getBundle()).append(SESSIONS).toFile()
                .toPath();
        AiCoreTokenProvider tokens = workbenchTokens();
        LoginModelProvider models = new LoginModelProvider(tokens);
        PromptAssembler prompt = new PromptAssembler(Instruction::getMessage, HistoryWindow::size);
        return new ChatRuntime(directory, models, prompt, new PreferenceActiveSessionStore(new EclipseMemory()),
                models::providerId, models::modelName, Clock.systemUTC());
    }

    /**
     * Builds the in-memory token provider. It requests tokens and does not save them.
     *
     * @return the provider.
     */
    private static AiCoreTokenProvider workbenchTokens() {
        AuthClient authClient = new AuthClient(TemporaryMemoryAccessToken.getInstance(), MemoryServiceKey.getInstance(),
                new AuthClientHelper());
        return new AiCoreTokenProvider(new SecureStore(), authClient, Clock.systemUTC());
    }
}
