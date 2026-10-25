package com.developer.nefarious.zjoule.plugin.core.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import com.developer.nefarious.zjoule.plugin.agent.model.ScriptedChat;
import com.developer.nefarious.zjoule.plugin.core.Activator;

/**
 * Preferences for when chat history is summarized.
 * <p>
 * Compaction runs when a session has more messages than the threshold. The
 * newest messages, up to the keep count, stay in the checkpoint. A keep count
 * that is not smaller than the threshold is treated as half the threshold so a
 * run still shortens the history.
 */
public abstract class CompactionLimits {

    /** Preference key for the message count that triggers compaction. */
    public static final String THRESHOLD_KEY = "compactionThreshold";

    /** Preference key for how many recent messages stay after compaction. */
    public static final String KEEP_KEY = "compactionKeep";

    /** Default message count that triggers compaction. */
    public static final int DEFAULT_THRESHOLD = 40;

    /** Default number of recent messages kept after compaction. */
    public static final int DEFAULT_KEEP = 20;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CompactionLimits() {
    }

    /**
     * Returns the configured threshold, or the default when the value is missing.
     *
     * @return a positive message count.
     */
    public static int threshold() {
        if (ScriptedChat.isEnabled()) {
            return ScriptedChat.THRESHOLD;
        }
        return positive(THRESHOLD_KEY, DEFAULT_THRESHOLD);
    }

    /**
     * Returns how many recent messages to keep, or the default when the value is missing.
     *
     * @return a positive message count.
     */
    public static int keep() {
        if (ScriptedChat.isEnabled()) {
            return ScriptedChat.KEEP;
        }
        return positive(KEEP_KEY, DEFAULT_KEEP);
    }

    /**
     * Reads a positive preference.
     *
     * @param key the preference key.
     * @param fallback the value used when the stored number is not positive.
     * @return the stored value, or the fallback.
     */
    private static int positive(final String key, final int fallback) {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        int value = store.getInt(key);
        if (value <= 0) {
            return fallback;
        }
        return value;
    }
}
