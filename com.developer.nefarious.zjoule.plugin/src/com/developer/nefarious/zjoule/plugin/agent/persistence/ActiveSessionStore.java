package com.developer.nefarious.zjoule.plugin.agent.persistence;

/**
 * Stores the id of the session shown in the chat view.
 */
public interface ActiveSessionStore {

    /**
     * Returns the active thread id.
     *
     * @return the id, or {@code null} when none is selected.
     */
    String get();

    /**
     * Selects a thread.
     *
     * @param threadId the thread id.
     */
    void set(final String threadId);
}
