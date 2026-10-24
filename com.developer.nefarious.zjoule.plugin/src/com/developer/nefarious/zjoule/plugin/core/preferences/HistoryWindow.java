package com.developer.nefarious.zjoule.plugin.core.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import com.developer.nefarious.zjoule.plugin.core.Activator;

/**
 * Preference for how many recent messages are sent to the model.
 */
public abstract class HistoryWindow {

    /** Preference key. */
    public static final String KEY = "historyWindow";

    /** Default window size. */
    public static final int DEFAULT = 20;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private HistoryWindow() {
    }

    /**
     * Returns the configured window, or the default when the value is missing.
     *
     * @return a positive window size.
     */
    public static int size() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        int value = store.getInt(KEY);
        if (value <= 0) {
            return DEFAULT;
        }
        return value;
    }
}
