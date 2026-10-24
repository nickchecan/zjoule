package com.developer.nefarious.zjoule.plugin.core.events;

import org.eclipse.swt.browser.Browser;

import com.developer.nefarious.zjoule.plugin.auth.SessionManager;
import com.developer.nefarious.zjoule.plugin.memory.EclipseMemory;

/**
 * Restores the login state and, when logged in, the active chat.
 */
public class Initialization implements Runnable {

    /** The {@link Browser} instance to interact with during initialization. */
    private Browser browser;

    /**
     * Constructs an {@code Initialization} instance with the specified {@link Browser}.
     *
     * @param browser the {@link Browser} instance used during initialization.
     */
    public Initialization(final Browser browser) {
        this.browser = browser;
    }

    /**
     * Logs the user in and renders history, or logs the user out.
     * History is rendered only from {@link SessionManager#login(Browser)}.
     */
    @Override
    public void run() {
        if (SessionManager.isUserLoggedIn()) {
            SessionManager.login(browser);
        } else {
            SessionManager.logout(browser, new EclipseMemory());
        }
        browser.execute("updatePlaceholder();");
    }
}
