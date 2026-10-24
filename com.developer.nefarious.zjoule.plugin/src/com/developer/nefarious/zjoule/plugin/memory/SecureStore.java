package com.developer.nefarious.zjoule.plugin.memory;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

import com.developer.nefarious.zjoule.plugin.memory.utils.ObjectSerializer;
import com.developer.nefarious.zjoule.plugin.models.ServiceKey;

/**
 * Stores the SAP service key in Eclipse secure storage.
 * Access tokens are not stored here.
 */
public final class SecureStore {

    /** Secure-storage node. Must not contain a slash. */
    public static final String NODE = "com.developer.nefarious.zjoule";

    /** Encrypted value key. Distinct from the legacy preference key. */
    public static final String KEY = "serviceKey";

    private final ISecurePreferences node;

    private final ObjectSerializer serializer = new ObjectSerializer();

    /**
     * Uses the default Eclipse secure-preferences node.
     */
    public SecureStore() {
        this(SecurePreferencesFactory.getDefault().node(NODE));
    }

    /**
     * Uses an explicit node. Tests pass a stand-in.
     *
     * @param node the secure-preferences node.
     */
    public SecureStore(final ISecurePreferences node) {
        this.node = node;
    }

    /**
     * Encrypts and stores the service key.
     *
     * @param key the service key.
     */
    public void putServiceKey(final ServiceKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Service key is required");
        }
        try {
            node.put(KEY, serializer.serialize(key), true);
            node.flush();
        } catch (StorageException | IOException ex) {
            throw new IllegalStateException("Could not store the service key", ex);
        }
    }

    /**
     * Loads the service key.
     *
     * @return the key, or empty when it is missing, blank, or unreadable.
     */
    public Optional<ServiceKey> serviceKey() {
        try {
            return parse(node.get(KEY, null));
        } catch (StorageException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * Removes the stored service key.
     */
    public void clear() {
        try {
            node.remove(KEY);
            node.flush();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not clear the service key", ex);
        }
    }

    /**
     * Parses a stored JSON value.
     *
     * @param json the stored JSON, or {@code null}.
     * @return the key when it is usable.
     */
    private Optional<ServiceKey> parse(final String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        ServiceKey key = serializer.deserialize(json, ServiceKey.class);
        if (key == null || !Boolean.TRUE.equals(key.isValid())) {
            return Optional.empty();
        }
        return Optional.of(key);
    }
}
