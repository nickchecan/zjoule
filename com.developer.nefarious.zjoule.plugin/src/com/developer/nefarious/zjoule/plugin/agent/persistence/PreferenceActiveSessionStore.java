package com.developer.nefarious.zjoule.plugin.agent.persistence;

import com.developer.nefarious.zjoule.plugin.memory.IEclipseMemory;

/**
 * Stores the active session id in the plug-in instance preferences.
 */
public final class PreferenceActiveSessionStore implements ActiveSessionStore {

    /** Preference key. Not a secret. */
    public static final String KEY = "zjoule.activeSession";

    private final IEclipseMemory memory;

    /**
     * Creates the store.
     *
     * @param memory the Eclipse preference store.
     */
    public PreferenceActiveSessionStore(final IEclipseMemory memory) {
        this.memory = memory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String get() {
        return memory.loadFromEclipsePreferences(KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(final String threadId) {
        memory.saveOnEclipsePreferences(KEY, threadId);
    }
}
