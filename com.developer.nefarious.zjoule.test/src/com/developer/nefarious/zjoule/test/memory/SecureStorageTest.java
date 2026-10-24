package com.developer.nefarious.zjoule.test.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.junit.jupiter.api.Test;

import com.developer.nefarious.zjoule.plugin.memory.EclipseMemory;
import com.developer.nefarious.zjoule.plugin.memory.LegacySecretCleaner;
import com.developer.nefarious.zjoule.plugin.memory.MemoryServiceKey;
import com.developer.nefarious.zjoule.plugin.memory.SecureStore;
import com.developer.nefarious.zjoule.plugin.models.ServiceKey;
import com.google.gson.Gson;

public class SecureStorageTest {

    private static final String SERVICE_KEY_JSON = "{"
            + "\"serviceurls\": {\"AI_API_URL\": \"https://api.example\"}, "
            + "\"clientid\": \"client\", "
            + "\"clientsecret\": \"secret\", "
            + "\"url\": \"https://auth.example\""
            + "}";

    @Test
    public void shouldRoundTripTheServiceKeyAndIgnoreCorruptJson() throws Exception {
        Map<String, String> values = new HashMap<>();
        SecureStore store = new SecureStore(node(values));
        ServiceKey key = new Gson().fromJson(SERVICE_KEY_JSON, ServiceKey.class);

        store.putServiceKey(key);

        assertEquals("client", store.serviceKey().orElseThrow().getClientId());
        assertTrue(Boolean.TRUE.equals(store.serviceKey().orElseThrow().isValid()));
        values.put(SecureStore.KEY, "{not json");
        assertTrue(store.serviceKey().isEmpty());
    }

    @Test
    public void shouldKeepTheServiceKeyOutOfInstancePreferences() throws Exception {
        EclipseMemory preferences = new EclipseMemory();
        preferences.saveOnEclipsePreferences(LegacySecretCleaner.SERVICE_KEY, "plain-secret");
        preferences.saveOnEclipsePreferences(LegacySecretCleaner.ACCESS_TOKEN, "plain-token");
        preferences.saveOnEclipsePreferences(LegacySecretCleaner.TEMP_SERVICE_KEY, "tmp-secret");
        preferences.saveOnEclipsePreferences(LegacySecretCleaner.TEMP_ACCESS_TOKEN, "tmp-token");

        LegacySecretCleaner.erase(preferences);

        assertNull(preferences.loadFromEclipsePreferences(LegacySecretCleaner.SERVICE_KEY));
        assertNull(preferences.loadFromEclipsePreferences(LegacySecretCleaner.ACCESS_TOKEN));
        assertNull(preferences.loadFromEclipsePreferences(LegacySecretCleaner.TEMP_SERVICE_KEY));
        assertNull(preferences.loadFromEclipsePreferences(LegacySecretCleaner.TEMP_ACCESS_TOKEN));

        Map<String, String> values = new HashMap<>();
        MemoryServiceKey.resetInstance();
        MemoryServiceKey.initialize(new SecureStore(node(values)));
        MemoryServiceKey.getInstance().save(new Gson().fromJson(SERVICE_KEY_JSON, ServiceKey.class));

        assertNull(preferences.loadFromEclipsePreferences(LegacySecretCleaner.SERVICE_KEY));
        assertNull(preferences.loadFromEclipsePreferences(LegacySecretCleaner.ACCESS_TOKEN));
        assertEquals("client", MemoryServiceKey.getInstance().load().getClientId());
        assertTrue(values.containsKey(SecureStore.KEY));
    }

    private static ISecurePreferences node(final Map<String, String> values) throws Exception {
        ISecurePreferences node = mock(ISecurePreferences.class);
        when(node.get(anyString(), nullable(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String fallback = invocation.getArgument(1);
            return values.getOrDefault(key, fallback);
        });
        doAnswer(invocation -> {
            values.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(node).put(anyString(), anyString(), anyBoolean());
        doAnswer(invocation -> {
            values.remove(invocation.getArgument(0));
            return null;
        }).when(node).remove(anyString());
        return node;
    }
}
