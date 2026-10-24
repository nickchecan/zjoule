package com.developer.nefarious.zjoule.plugin.login.memory;

import com.developer.nefarious.zjoule.plugin.memory.IMemoryObject;
import com.developer.nefarious.zjoule.plugin.memory.LegacySecretCleaner;
import com.developer.nefarious.zjoule.plugin.models.AccessToken;

/**
 * Holds an access token in memory during the login wizard.
 * {@link #persist()} drops it. Tokens are not written to preferences.
 */
public class TemporaryMemoryAccessToken implements IMemoryObject<AccessToken>, ITemporaryMemoryObject {

    /** Singleton instance of {@code TemporaryMemoryAccessToken}. */
    private static TemporaryMemoryAccessToken instance;

    /** Legacy preference key. No longer written. */
    public static final String KEY = LegacySecretCleaner.TEMP_ACCESS_TOKEN;

    /** Wizard value. Never flushed to preferences or secure storage. */
    private AccessToken current;

    /**
     * Retrieves the singleton instance of {@code TemporaryMemoryAccessToken}.
     *
     * @return the singleton instance.
     * @throws IllegalStateException if the instance has not been initialized.
     */
    public static TemporaryMemoryAccessToken getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TemporaryMemoryAccessToken not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Initializes the singleton. No preference store is required.
     */
    public static void initialize() {
        if (instance == null) {
            instance = new TemporaryMemoryAccessToken();
        }
    }

    /**
     * Resets the singleton instance. Useful for testing or reinitialization.
     */
    public static void resetInstance() {
        instance = null;
    }

    /**
     * Private constructor to enforce singleton behavior.
     */
    private TemporaryMemoryAccessToken() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isEmpty() {
        AccessToken accessToken = load();
        if ((accessToken == null) || (accessToken.getAccessToken() == null) || accessToken.getAccessToken().isEmpty()
                || accessToken.getAccessToken().isBlank()) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken load() {
        return current;
    }

    /**
     * Drops the wizard token. Nothing is written to disk.
     */
    @Override
    public void persist() {
        current = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final AccessToken accessToken) {
        current = accessToken;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        current = null;
    }
}
