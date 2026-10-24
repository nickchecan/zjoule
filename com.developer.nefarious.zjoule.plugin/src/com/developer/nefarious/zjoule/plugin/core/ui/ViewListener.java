package com.developer.nefarious.zjoule.plugin.core.ui;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.part.ViewPart;

import com.developer.nefarious.zjoule.plugin.core.events.Initialization;
import com.developer.nefarious.zjoule.plugin.core.events.PartListener;
import com.developer.nefarious.zjoule.plugin.core.events.SelectionListener;
import com.developer.nefarious.zjoule.plugin.core.functions.CancelHandler;
import com.developer.nefarious.zjoule.plugin.core.functions.ClearHandler;
import com.developer.nefarious.zjoule.plugin.core.functions.LoginHandler;
import com.developer.nefarious.zjoule.plugin.core.functions.LogoutHandler;
import com.developer.nefarious.zjoule.plugin.core.functions.PreferencesHandler;
import com.developer.nefarious.zjoule.plugin.core.functions.PromptHandler;
import com.developer.nefarious.zjoule.plugin.core.utils.SystemProvider;

import jakarta.inject.Inject;

/**
 * Represents a custom Eclipse view that integrates a browser and various event listeners.
 * <p>
 * The {@code ViewListener} class extends {@link ViewPart} and provides functionality for:
 * <ul>
 *   <li>Rendering HTML-based UI components using a {@link Browser}.</li>
 *   <li>Handling toolbar and menu actions like login, logout, and clearing chat history.</li>
 *   <li>Listening to part and selection events in the Eclipse workspace.</li>
 * </ul>
 */
public class ViewListener extends ViewPart {

    /** The Eclipse {@link Shell} for managing UI dialogs and components. */
    @Inject
    private Shell shell;

    /** The browser component for rendering HTML and JavaScript-based UI. */
    private Browser browser;

    /** Listener for selection events in the Eclipse workspace. */
    private ISelectionListener selectionListener;

    /** Listener for part lifecycle events in the Eclipse workspace. */
    private PartListener partListener;

    /**
     * Creates the view's controls, including the browser and toolbar.
     *
     * @param parent the parent {@link Composite} that will contain the view's controls.
     */
    @Override
    public void createPartControl(final Composite parent) {
    	int style = (isWindows()) ? SWT.EDGE : SWT.WEBKIT;
        browser = BrowserFactory.create(parent, style);
        selectionListener = SelectionListener.create(browser);
        IViewRender viewRender = ViewRender.create();
        partListener = PartListener.create(browser);

        browser.setText(viewRender.build());

        BrowserFunction promptHandler = PromptHandler.create(browser, "getAIResponse");
        BrowserFunction cancelHandler = CancelHandler.create(browser, "cancelAIResponse");
        browser.addDisposeListener(e -> {
            if (!browser.isDisposed()) {
                promptHandler.dispose();
                cancelHandler.dispose();
            }
        });

        getSite().getPage().addPartListener(partListener);
        getSite().getPage().addSelectionListener(selectionListener);
        Display.getDefault().asyncExec(new Initialization(browser));

        setUpToolbar();
    }
    
    /**
     * Checks if the current operating system is a version of Windows.
     * <p>
     * This method determines the operating system by checking if the string
     * returned by {@link SystemProvider#getCurrentSystem()} contains the substring "win"
     * (case insensitive).
     * </p>
     *
     * @return {@code true} if the operating system name contains "win", indicating a Windows OS;
     *         {@code false} otherwise.
     * @see SystemProvider#getCurrentSystem()
     */
    private boolean isWindows() {
		return SystemProvider.getCurrentSystem().toLowerCase().contains("win");
	}

	/**
     * Disposes of the view and its associated resources, including listeners and the browser.
     */
    @Override
    public void dispose() {
        if (selectionListener != null) {
            getSite().getPage().removeSelectionListener(selectionListener);
        }

        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
        }

        if (partListener != null) {
            getSite().getPage().removePartListener(partListener);
        }

        super.dispose();
    }

    /**
     * Sets the focus to the browser component.
     */
    @Override
    public void setFocus() {
        browser.setFocus();
    }

    /**
     * Sets up the toolbar and menu actions for the view.
     * <p>
     * Adds login, logout, and clear chat actions to the toolbar and menu.
     */
    private void setUpToolbar() {
        IToolBarManager toolbar = getToolbar();
        toolbar.add(LoginHandler.create(shell, browser));
        IMenuManager menu = getMenu();
        menu.add(ClearHandler.create(browser));
        menu.add(PreferencesHandler.create());
        menu.add(new Separator());
        menu.add(LogoutHandler.create(browser));
    }

    /**
     * Retrieves the menu manager for the view's menu.
     *
     * @return the {@link IMenuManager} for the view's menu.
     */
    private IMenuManager getMenu() {
        return getViewSite().getActionBars().getMenuManager();
    }

    /**
     * Retrieves the toolbar manager for the view's toolbar.
     *
     * @return the {@link IToolBarManager} for the view's toolbar.
     */
    private IToolBarManager getToolbar() {
        return getViewSite().getActionBars().getToolBarManager();
    }

    /**
     * Sets the browser component for the view.
     *
     * @param browser the {@link Browser} instance to set.
     */
    public void setBrowser(final Browser browser) {
        this.browser = browser;
    }

    /**
     * Sets the part listener for monitoring part lifecycle events.
     *
     * @param partListener the {@link PartListener} to set.
     */
    public void setPartListener(final PartListener partListener) {
        this.partListener = partListener;
    }

    /**
     * Sets the selection listener for monitoring selection events.
     *
     * @param selectionListener the {@link ISelectionListener} to set.
     */
    public void setSelectionListener(final ISelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    /**
     * Sets the shell instance for the view.
     *
     * @param shell the {@link Shell} to set.
     */
    public void setShell(final Shell shell) {
        this.shell = shell;
    }
}
