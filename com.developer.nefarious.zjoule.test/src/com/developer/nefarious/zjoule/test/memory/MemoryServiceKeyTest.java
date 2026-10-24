package com.developer.nefarious.zjoule.test.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.developer.nefarious.zjoule.plugin.memory.IMemoryObject;
import com.developer.nefarious.zjoule.plugin.memory.MemoryServiceKey;
import com.developer.nefarious.zjoule.plugin.memory.SecureStore;
import com.developer.nefarious.zjoule.plugin.models.ServiceKey;

public class MemoryServiceKeyTest {

    private IMemoryObject<ServiceKey> cut;

    @Mock
    private SecureStore mockSecureStore;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        MemoryServiceKey.resetInstance();
        MemoryServiceKey.initialize(mockSecureStore);
        cut = spy(MemoryServiceKey.getInstance());
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
    public void shouldNotBeEmptyWhenServiceKeyIsSave() {
        ServiceKey serviceKey = new ServiceKey();
        serviceKey.setClientId("some-service-key");
        doReturn(serviceKey).when(cut).load();

        assertFalse(cut.isEmpty());
    }

    @Test
    public void shouldLoadServiceKeyFromSecureStorage() {
        ServiceKey expected = new ServiceKey();
        when(mockSecureStore.serviceKey()).thenReturn(Optional.of(expected));

        assertEquals(expected, cut.load());
    }

    @Test
    public void shouldReturnNullWhenSecureStorageIsEmpty() {
        when(mockSecureStore.serviceKey()).thenReturn(Optional.empty());

        assertNull(cut.load());
    }

    @Test
    public void shouldSaveServiceKeyToSecureStorage() {
        ServiceKey serviceKey = new ServiceKey();

        cut.save(serviceKey);

        verify(mockSecureStore).putServiceKey(serviceKey);
    }

    @Test
    public void shouldEraseFromSecureStorage() {
        cut.clear();

        verify(mockSecureStore).clear();
    }

}
