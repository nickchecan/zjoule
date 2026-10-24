package com.developer.nefarious.zjoule.test.login.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.developer.nefarious.zjoule.plugin.login.memory.TemporaryMemoryAccessToken;
import com.developer.nefarious.zjoule.plugin.memory.EclipseMemory;
import com.developer.nefarious.zjoule.plugin.models.AccessToken;

public class TemporaryMemoryAccessTokenTest {

    private TemporaryMemoryAccessToken cut;

    @BeforeEach
    public void setUp() {
        TemporaryMemoryAccessToken.resetInstance();
        TemporaryMemoryAccessToken.initialize();
        cut = spy(TemporaryMemoryAccessToken.getInstance());
    }

    @Test
    public void shouldBeEmptyWhenAccessTokenIsBlank() {
        AccessToken token = new AccessToken();
        token.setAccessToken(" ");
        doReturn(token).when(cut).load();

        assertTrue(cut.isEmpty());
        verify(cut).load();
    }

    @Test
    public void shouldBeEmptyWhenAccessTokenIsEmpty() {
        AccessToken token = new AccessToken();
        token.setAccessToken("");
        doReturn(token).when(cut).load();

        assertTrue(cut.isEmpty());
    }

    @Test
    public void shouldBeEmptyWhenAccessTokenIsNull() {
        AccessToken token = new AccessToken();
        token.setAccessToken(null);
        doReturn(token).when(cut).load();

        assertTrue(cut.isEmpty());
    }

    @Test
    public void shouldKeepTheTokenInMemoryUntilPersistDropsIt() {
        AccessToken token = new AccessToken();
        token.setAccessToken("wizard-token");
        EclipseMemory preferences = new EclipseMemory();

        cut.save(token);

        assertEquals(token, cut.load());
        cut.persist();

        assertNull(cut.load());
        assertNull(preferences.loadFromEclipsePreferences(TemporaryMemoryAccessToken.KEY));
        assertNull(preferences.loadFromEclipsePreferences("access-token"));
    }

    @Test
    public void shouldClearTheInMemoryToken() {
        AccessToken token = new AccessToken();
        token.setAccessToken("wizard-token");
        cut.save(token);

        cut.clear();

        assertNull(cut.load());
    }

}
