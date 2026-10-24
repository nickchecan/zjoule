package com.developer.nefarious.zjoule.plugin.login.memory;

import com.developer.nefarious.zjoule.plugin.memory.IMemoryObject;
import com.developer.nefarious.zjoule.plugin.memory.LegacySecretCleaner;
import com.developer.nefarious.zjoule.plugin.memory.SecureStore;
import com.developer.nefarious.zjoule.plugin.models.ServiceKey;

/**
 * Holds a service key in memory during login, then writes it to secure storage.
 */
public class TemporaryMemoryServiceKey implements IMemoryObject<ServiceKey>, ITemporaryMemoryObject {

    /** Singleton instance of {@code TemporaryMemoryServiceKey}. */
    private static TemporaryMemoryServiceKey instance;

    /** Legacy preference key. No longer written. */
    public static final String KEY = LegacySecretCleaner.TEMP_SERVICE_KEY;

    /** Encrypted store used by {@link #persist()}. */
    private SecureStore secureStore;

    /** Wizard value. Never flushed to preferences. */
    private ServiceKey current;

    /**
     * Retrieves the singleton instance of {@code TemporaryMemoryServiceKey}.
     *
     * @return the singleton instance.
     * @throws IllegalStateException if the instance has not been initialized.
     */
    public static TemporaryMemoryServiceKey getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TemporaryMemoryServiceKey not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Initializes the singleton.
     *
     * @param secureStore the encrypted store that receives {@link #persist()}.
     */
    public static void initialize(final SecureStore secureStore) {
        if (instance == null) {
            instance = new TemporaryMemoryServiceKey(secureStore);
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
     *
     * @param secureStore the encrypted store that receives {@link #persist()}.
     */
    private TemporaryMemoryServiceKey(final SecureStore secureStore) {
        this.secureStore = secureStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isEmpty() {
        ServiceKey serviceKey = load();
        if ((serviceKey == null) || (serviceKey.getClientId() == null) || serviceKey.getClientId().isEmpty()
                || serviceKey.getClientId().isBlank()) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceKey load() {
        return current;
    }

    /**
     * Writes the in-memory key to secure storage.
     */
    @Override
    public void persist() {
        if (current != null) {
            secureStore.putServiceKey(current);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final ServiceKey serviceKey) {
        current = serviceKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        current = null;
    }
}
