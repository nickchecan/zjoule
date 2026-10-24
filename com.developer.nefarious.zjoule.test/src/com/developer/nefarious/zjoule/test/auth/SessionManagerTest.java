package com.developer.nefarious.zjoule.test.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.swt.browser.Browser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.developer.nefarious.zjoule.plugin.agent.persistence.PreferenceActiveSessionStore;
import com.developer.nefarious.zjoule.plugin.auth.SessionManager;
import com.developer.nefarious.zjoule.plugin.core.functions.TagHandler;
import com.developer.nefarious.zjoule.plugin.memory.EclipseMemory;
import com.developer.nefarious.zjoule.plugin.memory.MemoryDeployment;
import com.developer.nefarious.zjoule.plugin.memory.MemoryOllamaEndpoint;
import com.developer.nefarious.zjoule.plugin.memory.MemoryOllamaModel;
import com.developer.nefarious.zjoule.plugin.memory.MemoryResourceGroup;
import com.developer.nefarious.zjoule.plugin.memory.MemoryServiceKey;

public class SessionManagerTest {

    private MockedStatic<MemoryServiceKey> mockedStaticMemoryServiceKey;

    private MockedStatic<MemoryResourceGroup> mockedStaticMemoryResourceGroup;

    private MockedStatic<MemoryDeployment> mockedStaticMemoryDeployment;

    private MockedStatic<MemoryOllamaEndpoint> mockedStaticMemoryOllamaEndpoint;

    private MockedStatic<MemoryOllamaModel> mockedStaticMemoryOllamaModel;

    private MockedStatic<TagHandler> mockedStaticTagHandler;

    @Mock
    private MemoryServiceKey mockMemoryServiceKey;

    @Mock
    private MemoryResourceGroup mockMemoryResourceGroup;

    @Mock
    private MemoryDeployment mockMemoryDeployment;

    @Mock
    private MemoryOllamaEndpoint mockMemoryOllamaEndpoint;

    @Mock
    private MemoryOllamaModel mockMemoryOllamaModel;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockedStaticMemoryServiceKey = mockStatic(MemoryServiceKey.class);
        mockedStaticMemoryResourceGroup = mockStatic(MemoryResourceGroup.class);
        mockedStaticMemoryDeployment = mockStatic(MemoryDeployment.class);
        mockedStaticMemoryOllamaEndpoint = mockStatic(MemoryOllamaEndpoint.class);
        mockedStaticMemoryOllamaModel = mockStatic(MemoryOllamaModel.class);
        mockedStaticTagHandler = mockStatic(TagHandler.class);

        mockedStaticMemoryServiceKey.when(MemoryServiceKey::getInstance).thenReturn(mockMemoryServiceKey);
        mockedStaticMemoryResourceGroup.when(MemoryResourceGroup::getInstance).thenReturn(mockMemoryResourceGroup);
        mockedStaticMemoryDeployment.when(MemoryDeployment::getInstance).thenReturn(mockMemoryDeployment);
        mockedStaticMemoryOllamaEndpoint.when(MemoryOllamaEndpoint::getInstance).thenReturn(mockMemoryOllamaEndpoint);
        mockedStaticMemoryOllamaModel.when(MemoryOllamaModel::getInstance).thenReturn(mockMemoryOllamaModel);

        when(mockMemoryServiceKey.isEmpty()).thenReturn(true);
        when(mockMemoryResourceGroup.isEmpty()).thenReturn(true);
        when(mockMemoryDeployment.isEmpty()).thenReturn(true);
        when(mockMemoryOllamaEndpoint.isEmpty()).thenReturn(true);
        when(mockMemoryOllamaModel.isEmpty()).thenReturn(true);
    }

    @Test
    public void shouldLogin() {
        Browser mockBrowser = mock(Browser.class);

        SessionManager.login(mockBrowser);

        verify(mockBrowser).execute("login();");
        mockedStaticTagHandler.verify(() -> TagHandler.update(mockBrowser));
    }

    @Test
    public void shouldLogout() {
        Browser mockBrowser = mock(Browser.class);
        EclipseMemory mockEclipseMemory = mock(EclipseMemory.class);

        SessionManager.logout(mockBrowser, mockEclipseMemory);

        verify(mockEclipseMemory).clearAll();
        verify(mockEclipseMemory, never()).saveOnEclipsePreferences(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(mockMemoryServiceKey).clear();
        verify(mockBrowser).execute("logout();");
    }

    @Test
    public void shouldRestoreActiveSessionAfterLogoutClearsPreferences() {
        Browser mockBrowser = mock(Browser.class);
        EclipseMemory mockEclipseMemory = mock(EclipseMemory.class);
        when(mockEclipseMemory.loadFromEclipsePreferences(PreferenceActiveSessionStore.KEY)).thenReturn("session-1");

        SessionManager.logout(mockBrowser, mockEclipseMemory);

        verify(mockEclipseMemory).clearAll();
        verify(mockEclipseMemory).saveOnEclipsePreferences(PreferenceActiveSessionStore.KEY, "session-1");
        verify(mockMemoryServiceKey).clear();
    }

    @Test
    public void shouldReturnFalseWhenNothingIsStored() {
        assertFalse(SessionManager.isUserLoggedIn());
    }

    @Test
    public void shouldReturnFalseWhenServiceKeyIsMissing() {
        when(mockMemoryResourceGroup.isEmpty()).thenReturn(false);
        when(mockMemoryDeployment.isEmpty()).thenReturn(false);

        assertFalse(SessionManager.isUserLoggedIn());
    }

    @Test
    public void shouldReturnFalseWhenResourceGroupIsMissing() {
        when(mockMemoryServiceKey.isEmpty()).thenReturn(false);
        when(mockMemoryDeployment.isEmpty()).thenReturn(false);

        assertFalse(SessionManager.isUserLoggedIn());
    }

    @Test
    public void shouldReturnFalseWhenDeploymentIsMissing() {
        when(mockMemoryServiceKey.isEmpty()).thenReturn(false);
        when(mockMemoryResourceGroup.isEmpty()).thenReturn(false);

        assertFalse(SessionManager.isUserLoggedIn());
    }

    @Test
    public void shouldReturnTrueWhenSapSessionHasNoStoredToken() {
        when(mockMemoryServiceKey.isEmpty()).thenReturn(false);
        when(mockMemoryResourceGroup.isEmpty()).thenReturn(false);
        when(mockMemoryDeployment.isEmpty()).thenReturn(false);

        assertTrue(SessionManager.isUserLoggedIn());
    }

    @Test
    public void shouldReturnTrueForOllamaWithoutSapCredentials() {
        when(mockMemoryOllamaEndpoint.isEmpty()).thenReturn(false);
        when(mockMemoryOllamaModel.isEmpty()).thenReturn(false);

        assertTrue(SessionManager.isUserLoggedIn());
    }

    //CHECKSTYLE:OFF CyclomaticComplexity
    @AfterEach
    public void tearDown() {
        mockedStaticMemoryServiceKey.close();
        mockedStaticMemoryResourceGroup.close();
        mockedStaticMemoryDeployment.close();
        mockedStaticMemoryOllamaEndpoint.close();
        mockedStaticMemoryOllamaModel.close();
        mockedStaticTagHandler.close();
    }
    //CHECKSTYLE:ON CyclomaticComplexity

}
