package com.developer.nefarious.zjoule.plugin.agent;

import java.util.List;
import java.util.Optional;

import com.developer.nefarious.zjoule.plugin.agent.persistence.SessionInfo;

import dev.langchain4j.data.message.ChatMessage;

/**
 * The chat view's entry point. Message history is read from the checkpointer.
 */
public interface ChatSessionService {

    /**
     * Creates an empty session and makes it active.
     *
     * @return the new session.
     */
    SessionInfo newSession();

    /**
     * Lists sessions, newest update first.
     *
     * @return the sessions.
     */
    List<SessionInfo> list();

    /**
     * Returns the session currently shown in the view.
     *
     * @return the active session.
     */
    Optional<SessionInfo> active();

    /**
     * Selects an existing session.
     *
     * @param threadId the thread id.
     */
    void activate(final String threadId);

    /**
     * Returns the checkpoint history for a session.
     *
     * @param threadId the thread id.
     * @return the messages, or an empty list when the session has none.
     */
    List<ChatMessage> history(final String threadId);

    /**
     * Cancels a running turn, deletes the session file, and removes the index entry.
     * When the deleted session was active, a new empty session is selected.
     *
     * @param threadId the thread id.
     */
    void delete(final String threadId);

    /**
     * Sends a user message on a background job.
     *
     * @param threadId the thread id.
     * @param userText the user message.
     * @param editorContext the active editor text, or {@code null}.
     * @param listener the stream listener.
     * @return a handle that can cancel the turn.
     */
    RunHandle send(final String threadId, final String userText, final String editorContext,
            final ChatStreamListener listener);

    /**
     * Resumes a paused turn. Reserved for a later tools node.
     *
     * @param threadId the thread id.
     * @param approved whether the user approved the pending call.
     * @param listener the stream listener.
     * @return a handle that can cancel the turn.
     */
    RunHandle resume(final String threadId, final boolean approved, final ChatStreamListener listener);
}
