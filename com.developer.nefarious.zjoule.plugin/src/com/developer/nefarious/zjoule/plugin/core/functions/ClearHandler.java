package com.developer.nefarious.zjoule.plugin.core.functions;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.developer.nefarious.zjoule.plugin.auth.SessionManager;
import com.developer.nefarious.zjoule.plugin.core.Activator;

/**
 * Handles the "Clear Chat" action in the application.
 * <p>
 * This class extends {@link Action} and provides functionality to clear the chat message history
 * and reset the associated browser state. It also manages the display icon for the action.
 */
public class ClearHandler extends Action {

    /** Path to the icon used for the "Clear Chat" action. */
    private static final String ICON = "platform:/plugin/org.eclipse.ui/icons/full/etool16/clear.png";

    /** The {@link Browser} instance associated with the action. */
    private Browser browser;

    /**
     * Factory method to create a new {@code ClearHandler} instance.
     *
     * @param browser the {@link Browser} instance associated with the action.
     * @return a new {@code ClearHandler} instance.
     */
    public static ClearHandler create(final Browser browser) {
        return new ClearHandler(browser);
    }

    /**
     * Constructs a new {@code ClearHandler} instance.
     *
     * @param browser the {@link Browser} instance associated with the action.
     */
    private ClearHandler(final Browser browser) {
        this.browser = browser;

        setText("New chat");
        setToolTipText("Start a new chat. Previous chats are kept.");
        setIcon();
    }

    /**
     * Executes the "Clear Chat" action.
     * <p>
     * If the user is logged in, this method starts an empty session and resets the
     * browser. Previous session files are kept.
     */
    @Override
    public void run() {
        if (SessionManager.isUserLoggedIn()) {
            Activator.getDefault().chatSessions().newSession();
            browser.execute("clearMessages();");
            browser.execute("updatePlaceholder();");
        }
    }

    /**
     * Sets the icon for the action.
     * <p>
     * Attempts to load the icon from the specified path. If the icon cannot be loaded,
     * a default icon from Eclipse's shared images is used.
     */
    private void setIcon() {
        try {
            URI iconURI = new URI(ICON);
            URL iconURL = iconURI.toURL();
            setImageDescriptor(ImageDescriptor.createFromURL(iconURL));
        } catch (URISyntaxException | MalformedURLException e) {
            e.printStackTrace();
            setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
        }
    }
}
