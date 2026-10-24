package com.developer.nefarious.zjoule.test.login.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.developer.nefarious.zjoule.plugin.login.memory.TemporaryMemoryServiceKey;
import com.developer.nefarious.zjoule.plugin.memory.SecureStore;
import com.developer.nefarious.zjoule.plugin.models.ServiceKey;

public class TemporaryMemoryServiceKeyTest {

    private TemporaryMemoryServiceKey cut;

    @Mock
    private SecureStore mockSecureStore;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        TemporaryMemoryServiceKey.resetInstance();
        TemporaryMemoryServiceKey.initialize(mockSecureStore);
        cut = spy(TemporaryMemoryServiceKey.getInstance());
    }

    @Test
    public void shouldBeEmptyWhenNoMemory() {
        doReturn(null).when(cut).load();

        assertTrue(cut.isEmpty());
        verify(cut).load();
    }

    @Test
    public void shouldBeEmptyWhenServiceKeyIsBlank() {
        ServiceKey serviceKey = new ServiceKey();
        serviceKey.setClientId(" ");
        doReturn(serviceKey).when(cut).load();

        assertTrue(cut.isEmpty());
    }

    @Test
    public void shouldBeEmptyWhenServiceKeyIsEmpty() {
        ServiceKey serviceKey = new ServiceKey();
        serviceKey.setClientId("");
        doReturn(serviceKey).when(cut).load();

        assertTrue(cut.isEmpty());
    }

    @Test
    public void shouldBeEmptyWhenServiceKeyIsNull() {
        ServiceKey serviceKey = new ServiceKey();
        serviceKey.setClientId(null);
        doReturn(serviceKey).when(cut).load();

        assertTrue(cut.isEmpty());
    }

    @Test
    public void shouldHoldTheKeyInMemoryWithoutWritingSecureStorage() {
        ServiceKey serviceKey = new ServiceKey();
        serviceKey.setClientId("client");

        cut.save(serviceKey);

        assertEquals(serviceKey, cut.load());
        verify(mockSecureStore, never()).putServiceKey(serviceKey);
    }

    @Test
    public void shouldPersistTheInMemoryKeyToSecureStorage() {
        ServiceKey serviceKey = new ServiceKey();
        serviceKey.setClientId("client");
        cut.save(serviceKey);

        cut.persist();

        verify(mockSecureStore).putServiceKey(serviceKey);
    }

    @Test
    public void shouldSkipPersistWhenNothingWasEntered() {
        cut.persist();

        verify(mockSecureStore, never()).putServiceKey(org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void shouldClearTheInMemoryKey() {
        ServiceKey serviceKey = new ServiceKey();
        when(mockSecureStore.serviceKey()).thenReturn(Optional.of(serviceKey));
        cut.save(serviceKey);

        cut.clear();

        assertNull(cut.load());
        verify(mockSecureStore, never()).clear();
    }

}
