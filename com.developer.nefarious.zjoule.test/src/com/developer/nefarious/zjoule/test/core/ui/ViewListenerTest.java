package com.developer.nefarious.zjoule.test.core.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.developer.nefarious.zjoule.plugin.core.events.Initialization;
import com.developer.nefarious.zjoule.plugin.core.events.PartListener;
import com.developer.nefarious.zjoule.plugin.core.events.SelectionListener;
import com.developer.nefarious.zjoule.plugin.core.functions.CancelHandler;
import com.developer.nefarious.zjoule.plugin.core.functions.ClearHandler;
import com.developer.nefarious.zjoule.plugin.core.functions.LoginHandler;
import com.developer.nefarious.zjoule.plugin.core.functions.LogoutHandler;
import com.developer.nefarious.zjoule.plugin.core.functions.PreferencesHandler;
import com.developer.nefarious.zjoule.plugin.core.functions.PromptHandler;
import com.developer.nefarious.zjoule.plugin.core.ui.BrowserFactory;
import com.developer.nefarious.zjoule.plugin.core.ui.IViewRender;
import com.developer.nefarious.zjoule.plugin.core.ui.ViewListener;
import com.developer.nefarious.zjoule.plugin.core.ui.ViewRender;
import com.developer.nefarious.zjoule.plugin.core.utils.SystemProvider;

public class ViewListenerTest {

	private ViewListener cut;

	private MockedStatic<BrowserFactory> mockedStaticBrowserFactory;

	private MockedStatic<SelectionListener> mockedStaticSelectionListener;

	private MockedStatic<ViewRender> mockedStaticViewRender;

	private MockedStatic<PartListener> mockedStaticPartListener;

	private MockedStatic<PromptHandler> mockedStaticPromptHandler;

	private MockedStatic<CancelHandler> mockedStaticCancelHandler;

	private MockedStatic<Display> mockedStaticDisplay;

	private MockedStatic<LoginHandler> mockedStaticLoginHandler;

	private MockedStatic<ClearHandler> mockedStaticClearHandler;

	private MockedStatic<LogoutHandler> mockedStaticLogoutHandler;
	
	private MockedStatic<SystemProvider> mockedStaticSystemProvider;
	
	private MockedStatic<PreferencesHandler> mockedStaticPreferencesHandler;

	@Mock
	private Shell mockShell;

	@Mock
	private Browser mockBrowser;

	@Mock
	private ISelectionListener mockSelectionListener;

	@Mock
	private IViewRender mockViewRender;

	@Mock
	private PartListener mockPartListener;

	@Mock
	private Composite mockParent;

	@Mock
	private PromptHandler mockPromptHandler;

	@Mock
	private CancelHandler mockCancelHandler;

	@Mock
	private Display mockDisplay;

	@Mock
	private LoginHandler mockLoginHandler;

	@Mock
	private ClearHandler mockClearHandler;

	@Mock
	private LogoutHandler mockLogoutHandler;
	
	@Mock
	private PreferencesHandler mockPreferencesHandler;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		mockedStaticBrowserFactory = mockStatic(BrowserFactory.class);
		mockedStaticSelectionListener = mockStatic(SelectionListener.class);
		mockedStaticViewRender = mockStatic(ViewRender.class);
		mockedStaticPartListener = mockStatic(PartListener.class);
		mockedStaticPromptHandler = mockStatic(PromptHandler.class);
		mockedStaticCancelHandler = mockStatic(CancelHandler.class);
		mockedStaticDisplay = mockStatic(Display.class);
		mockedStaticLoginHandler = mockStatic(LoginHandler.class);
		mockedStaticClearHandler = mockStatic(ClearHandler.class);
		mockedStaticLogoutHandler = mockStatic(LogoutHandler.class);
		mockedStaticSystemProvider = mockStatic(SystemProvider.class);
		mockedStaticPreferencesHandler = mockStatic(PreferencesHandler.class);

		mockedStaticSelectionListener.when(() -> SelectionListener.create(mockBrowser)).thenReturn(mockSelectionListener);
		mockedStaticViewRender.when(ViewRender::create).thenReturn(mockViewRender);
		mockedStaticPartListener.when(() -> PartListener.create(mockBrowser)).thenReturn(mockPartListener);
		mockedStaticPromptHandler.when(() -> PromptHandler.create(mockBrowser, "getAIResponse")).thenReturn(mockPromptHandler);
		mockedStaticCancelHandler.when(() -> CancelHandler.create(mockBrowser, "cancelAIResponse")).thenReturn(mockCancelHandler);
		mockedStaticDisplay.when(Display::getDefault).thenReturn(mockDisplay);
		mockedStaticLoginHandler.when(() -> LoginHandler.create(mockShell, mockBrowser)).thenReturn(mockLoginHandler);
		mockedStaticClearHandler.when(() -> ClearHandler.create(mockBrowser)).thenReturn(mockClearHandler);
		mockedStaticLogoutHandler.when(() -> LogoutHandler.create(mockBrowser)).thenReturn(mockLogoutHandler);
		mockedStaticPreferencesHandler.when(PreferencesHandler::create).thenReturn(mockPreferencesHandler);

		cut = spy(new ViewListener());
		cut.setShell(mockShell);
	}
	
	@Test
	public void shouldPlumbPartControlForWindows() {
		// Arrange
		mockedStaticSystemProvider.when(SystemProvider::getCurrentSystem).thenReturn("Windows 95");
		mockedStaticBrowserFactory.when(() -> BrowserFactory.create(mockParent, SWT.EDGE)).thenReturn(mockBrowser);
		
		String mockBuildResult = "html-text";
		when(mockViewRender.build()).thenReturn(mockBuildResult);

		IWorkbenchPartSite mockSite = mock(IWorkbenchPartSite.class);
		doReturn(mockSite).when(cut).getSite();

		IWorkbenchPage mockPage = mock(IWorkbenchPage.class);
		when(mockSite.getPage()).thenReturn(mockPage);

		IViewSite mockViewSite = mock(IViewSite.class);
		doReturn(mockViewSite).when(cut).getViewSite();
		IActionBars mockActionBars = mock(IActionBars.class);
		when(mockViewSite.getActionBars()).thenReturn(mockActionBars);

		IToolBarManager mockToolBarManager = mock(IToolBarManager.class);
		when(mockActionBars.getToolBarManager()).thenReturn(mockToolBarManager);

		IMenuManager mockMenuManager = mock(IMenuManager.class);
		when(mockActionBars.getMenuManager()).thenReturn(mockMenuManager);

		// Act
		cut.createPartControl(mockParent);

		// Assert
		verify(mockBrowser).setText(mockBuildResult);
		verify(mockBrowser).addDisposeListener(any(DisposeListener.class));
		verify(mockPage).addPartListener(mockPartListener);
		verify(mockPage).addSelectionListener(mockSelectionListener);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockDisplay).asyncExec(captor.capture());
        Runnable actualRunnable = captor.getValue();
        assertTrue(actualRunnable instanceof Initialization);

        verify(mockToolBarManager).add(mockLoginHandler);
        verify(mockMenuManager).add(mockClearHandler);
        verify(mockMenuManager).add(mockPreferencesHandler);
        verify(mockMenuManager).add(any(Separator.class));
        verify(mockMenuManager).add(mockLogoutHandler);
	}

	@Test
	public void shouldPlumbPartControlForOtherSystems() {
		// Arrange
		mockedStaticSystemProvider.when(SystemProvider::getCurrentSystem).thenReturn("Mac OS");
		mockedStaticBrowserFactory.when(() -> BrowserFactory.create(mockParent, SWT.WEBKIT)).thenReturn(mockBrowser);
		
		String mockBuildResult = "html-text";
		when(mockViewRender.build()).thenReturn(mockBuildResult);

		IWorkbenchPartSite mockSite = mock(IWorkbenchPartSite.class);
		doReturn(mockSite).when(cut).getSite();

		IWorkbenchPage mockPage = mock(IWorkbenchPage.class);
		when(mockSite.getPage()).thenReturn(mockPage);

		IViewSite mockViewSite = mock(IViewSite.class);
		doReturn(mockViewSite).when(cut).getViewSite();
		IActionBars mockActionBars = mock(IActionBars.class);
		when(mockViewSite.getActionBars()).thenReturn(mockActionBars);

		IToolBarManager mockToolBarManager = mock(IToolBarManager.class);
		when(mockActionBars.getToolBarManager()).thenReturn(mockToolBarManager);

		IMenuManager mockMenuManager = mock(IMenuManager.class);
		when(mockActionBars.getMenuManager()).thenReturn(mockMenuManager);

		// Act
		cut.createPartControl(mockParent);

		// Assert
		verify(mockBrowser).setText(mockBuildResult);
		verify(mockBrowser).addDisposeListener(any(DisposeListener.class));
		verify(mockPage).addPartListener(mockPartListener);
		verify(mockPage).addSelectionListener(mockSelectionListener);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockDisplay).asyncExec(captor.capture());
        Runnable actualRunnable = captor.getValue();
        assertTrue(actualRunnable instanceof Initialization);

        verify(mockToolBarManager).add(mockLoginHandler);
        verify(mockMenuManager).add(mockClearHandler);
        verify(mockMenuManager).add(mockPreferencesHandler);
        verify(mockMenuManager).add(any(Separator.class));
        verify(mockMenuManager).add(mockLogoutHandler);
	}

	//CHECKSTYLE:OFF CyclomaticComplexity
	@AfterEach
	public void tearDown() {
		if (mockedStaticBrowserFactory != null) {
			mockedStaticBrowserFactory.close();
		}
		if (mockedStaticSelectionListener != null) {
			mockedStaticSelectionListener.close();
		}
		if (mockedStaticViewRender != null) {
			mockedStaticViewRender.close();
		}
		if (mockedStaticPartListener != null) {
			mockedStaticPartListener.close();
		}
		if (mockedStaticPromptHandler != null) {
			mockedStaticPromptHandler.close();
		}
		if (mockedStaticCancelHandler != null) {
			mockedStaticCancelHandler.close();
		}
		if (mockedStaticDisplay != null) {
			mockedStaticDisplay.close();
		}
		if (mockedStaticLoginHandler != null) {
			mockedStaticLoginHandler.close();
		}
		if (mockedStaticClearHandler != null) {
			mockedStaticClearHandler.close();
		}
		if (mockedStaticLogoutHandler != null) {
			mockedStaticLogoutHandler.close();
		}
		if (mockedStaticSystemProvider != null) {
			mockedStaticSystemProvider.close();
		}
		if (mockedStaticPreferencesHandler != null) {
			mockedStaticPreferencesHandler.close();
		}
	}
	//CHECKSTYLE:ON CyclomaticComplexity

	@Test
	public void testDispose() {
		// Arrange
		cut.setSelectionListener(mockSelectionListener);
		cut.setBrowser(mockBrowser);
		cut.setPartListener(mockPartListener);

		IWorkbenchPartSite mockSite = mock(IWorkbenchPartSite.class);
		doReturn(mockSite).when(cut).getSite();

		IWorkbenchPage mockPage = mock(IWorkbenchPage.class);
		when(mockSite.getPage()).thenReturn(mockPage);

		// Act
		cut.dispose();

		// Assert
		verify(mockPage).removeSelectionListener(mockSelectionListener);
		verify(mockBrowser).dispose();
		verify(mockPage).removePartListener(mockPartListener);
	}

	@Test
	public void testSetFocus() {
		// Arrange
		cut.setBrowser(mockBrowser);

		// Act
		cut.setFocus();

		// Assert
		verify(mockBrowser).setFocus();
	}

}
