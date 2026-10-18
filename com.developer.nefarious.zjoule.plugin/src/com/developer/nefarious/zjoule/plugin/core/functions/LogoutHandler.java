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
import com.developer.nefarious.zjoule.plugin.memory.EclipseMemory;

/**
 * Handles the "Logout" action for ending the user's session.
 * <p>
 * This class extends {@link Action} and manages the process of logging out the user
 * by invoking the {@link SessionManager#logout(Browser, EclipseMemory)} method.
 * It also configures the action's icon and tooltip text.
 */
public class LogoutHandler extends Action {

    /** Path to the icon used for the "Logout" action. */
    private static final String ICON = "platform:/plugin/org.eclipse.ui.workbench.texteditor/icons/full/elcl16/delete_template.png";

    /** The {@link Browser} instance associated with the logout action. */
    private Browser browser;

    /**
     * Factory method to create a new {@code LogoutHandler} instance.
     *
     * @param browser the {@link Browser} instance associated with the action.
     * @return a new {@code LogoutHandler} instance.
     */
    public static LogoutHandler create(final Browser browser) {
        return new LogoutHandler(browser);
    }

    /**
     * Constructs a new {@code LogoutHandler} instance.
     *
     * @param browser the {@link Browser} instance associated with the action.
     */
    private LogoutHandler(final Browser browser) {
        this.browser = browser;

        setText("Logout");
        setToolTipText("End Session.");
        setIcon();
    }

    /**
     * Executes the "Logout" action.
     * <p>
     * Invokes the {@link SessionManager#logout(Browser, EclipseMemory)} method
     * to end the user's session and clear associated memory.
     */
    @Override
    public void run() {
        SessionManager.logout(browser, new EclipseMemory());
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
            setImageDescriptor(
                    PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
        }
    }
}
