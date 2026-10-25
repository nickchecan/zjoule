package com.developer.nefarious.zjoule.plugin.core.functions;

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.browser.Browser;

import com.developer.nefarious.zjoule.plugin.agent.ChatSessionService;
import com.developer.nefarious.zjoule.plugin.agent.persistence.SessionInfo;
import com.developer.nefarious.zjoule.plugin.auth.SessionManager;
import com.developer.nefarious.zjoule.plugin.core.Activator;

/**
 * View menu for starting, opening, and deleting chat sessions.
 */
public final class SessionsMenu extends MenuManager {

    /** How many recent sessions the menu lists. */
    public static final int RECENT_LIMIT = 15;

    /** Menu item that starts an empty session. */
    static final String NEW_CHAT = "New chat";

    /** Menu item that deletes the session open in the view. */
    static final String DELETE_CURRENT = "Delete current";

    /**
     * Creates the menu. Items are rebuilt each time it is shown.
     *
     * @param browser the chat browser.
     */
    public SessionsMenu(final Browser browser) {
        super("Sessions");
        setRemoveAllWhenShown(true);
        addMenuListener(manager -> populate(browser));
    }

    /**
     * Returns the newest sessions, limited to {@link #RECENT_LIMIT}.
     * The input is already newest first.
     *
     * @param sessions the full list.
     * @return the sessions the menu shows.
     */
    public static List<SessionInfo> recent(final List<SessionInfo> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return List.of();
        }
        int limit = Math.min(RECENT_LIMIT, sessions.size());
        return List.copyOf(sessions.subList(0, limit));
    }

    /**
     * Fills the menu from the current index.
     *
     * @param browser the chat browser.
     */
    private void populate(final Browser browser) {
        add(action(NEW_CHAT, () -> newChat(browser)));
        add(new Separator());
        List<SessionInfo> sessions = recent(load());
        for (SessionInfo session : sessions) {
            String threadId = session.threadId();
            add(action(label(session), () -> open(browser, threadId)));
        }
        if (!sessions.isEmpty()) {
            add(new Separator());
        }
        add(action(DELETE_CURRENT, () -> deleteCurrent(browser)));
    }

    /**
     * Starts an empty session and clears the view. The previous file is kept.
     *
     * @param browser the chat browser.
     */
    private static void newChat(final Browser browser) {
        if (!SessionManager.isUserLoggedIn()) {
            return;
        }
        PromptHandler.cancelActiveResponse();
        Activator.getDefault().chatSessions().newSession();
        reset(browser);
    }

    /**
     * Opens a stored session in the view.
     *
     * @param browser the chat browser.
     * @param threadId the session id.
     */
    private static void open(final Browser browser, final String threadId) {
        if (!SessionManager.isUserLoggedIn() || browser == null || browser.isDisposed()) {
            return;
        }
        PromptHandler.cancelActiveResponse();
        ChatSessionService sessions = Activator.getDefault().chatSessions();
        sessions.activate(threadId);
        browser.execute("clearMessages();");
        BrowserStreamBridge.render(browser, sessions.history(threadId));
        browser.execute("updatePlaceholder();");
    }

    /**
     * Deletes the active session file and clears the view.
     *
     * @param browser the chat browser.
     */
    private static void deleteCurrent(final Browser browser) {
        if (!SessionManager.isUserLoggedIn()) {
            return;
        }
        ChatSessionService sessions = Activator.getDefault().chatSessions();
        Optional<SessionInfo> active = sessions.active();
        if (active.isEmpty()) {
            return;
        }
        sessions.delete(active.get().threadId());
        reset(browser);
    }

    /**
     * Clears the browser transcript.
     *
     * @param browser the chat browser.
     */
    private static void reset(final Browser browser) {
        if (browser == null || browser.isDisposed()) {
            return;
        }
        browser.execute("clearMessages();");
        browser.execute("updatePlaceholder();");
    }

    /**
     * Reads the session index. An empty list is used before the plug-in starts.
     *
     * @return newest first.
     */
    private static List<SessionInfo> load() {
        if (Activator.getDefault() == null) {
            return List.of();
        }
        return Activator.getDefault().chatSessions().list();
    }

    /**
     * Returns the menu label for one session.
     *
     * @param session the session.
     * @return the title.
     */
    private static String label(final SessionInfo session) {
        if (session.title() == null || session.title().isBlank()) {
            return SessionInfo.NEW_CHAT_TITLE;
        }
        return session.title();
    }

    /**
     * Creates a menu action.
     *
     * @param text the item label.
     * @param callback the action.
     * @return the action.
     */
    private static Action action(final String text, final Runnable callback) {
        return new Action(text) {
            @Override
            public void run() {
                callback.run();
            }
        };
    }
}
