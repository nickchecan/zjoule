package com.developer.nefarious.zjoule.plugin.memory;

/**
 * Deletes service-key and access-token values left in plain preferences.
 * Chat history is not stored in those keys, so this does not remove sessions.
 */
public final class LegacySecretCleaner {

    /** Legacy preference key for the service key. */
    public static final String SERVICE_KEY = "service-key";

    /** Legacy preference key for the access token. */
    public static final String ACCESS_TOKEN = "access-token";

    /** Legacy preference key for the wizard service key. */
    public static final String TEMP_SERVICE_KEY = "tmp-service-key";

    /** Legacy preference key for the wizard access token. */
    public static final String TEMP_ACCESS_TOKEN = "tmp-access-token";

    private LegacySecretCleaner() {
    }

    /**
     * Removes the four legacy keys. Safe to call on every start.
     *
     * @param memory the instance preferences.
     */
    public static void erase(final IEclipseMemory memory) {
        memory.deleteFromEclipsePreferences(SERVICE_KEY);
        memory.deleteFromEclipsePreferences(ACCESS_TOKEN);
        memory.deleteFromEclipsePreferences(TEMP_SERVICE_KEY);
        memory.deleteFromEclipsePreferences(TEMP_ACCESS_TOKEN);
    }
}
