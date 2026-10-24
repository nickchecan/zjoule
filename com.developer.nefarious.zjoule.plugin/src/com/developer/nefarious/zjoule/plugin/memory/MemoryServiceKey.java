package com.developer.nefarious.zjoule.plugin.memory;

import com.developer.nefarious.zjoule.plugin.models.ServiceKey;

/**
 * Stores the service key in Eclipse secure storage.
 */
public class MemoryServiceKey implements IMemoryObject<ServiceKey> {

    /** Legacy preference key. Deleted on startup and no longer written. */
    public static final String KEY = LegacySecretCleaner.SERVICE_KEY;

    /** Singleton instance of {@code MemoryServiceKey}. */
    private static MemoryServiceKey instance;

    /** Encrypted store for the service key. */
    private SecureStore secureStore;

    /**
     * Retrieves the singleton instance of {@code MemoryServiceKey}.
     *
     * @return the singleton instance.
     * @throws IllegalStateException if the instance has not been initialized.
     */
    public static MemoryServiceKey getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MemoryServiceKey not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Initializes the singleton with the secure store.
     *
     * @param secureStore the encrypted service-key store.
     */
    public static void initialize(final SecureStore secureStore) {
        if (instance == null) {
            instance = new MemoryServiceKey(secureStore);
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
     * @param secureStore the encrypted service-key store.
     */
    private MemoryServiceKey(final SecureStore secureStore) {
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
        return secureStore.serviceKey().orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final ServiceKey serviceKey) {
        secureStore.putServiceKey(serviceKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        secureStore.clear();
    }
}
