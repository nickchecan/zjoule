package com.developer.nefarious.zjoule.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit5.SWTBotJunit5Extension;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Workbench checks for the zJoule view, login wizard, and preference page.
 * <p>
 * The suite drives a real Eclipse workbench through SWTBot. It does not call
 * SAP AI Core, Ollama, or any other network service, and it does not need ABAP
 * Development Tools. The view is matched by its contribution id. The browser
 * document title is not asserted, so a rename of that title does not fail the
 * suite. Show View accepts either {@code zjoule} or {@code zJoule}.
 * </p>
 */
@ExtendWith(SWTBotJunit5Extension.class)
public class ZJouleWorkbenchUiTest {

    /** Contribution id from plugin.xml. This stays stable across title edits. */
    private static final String VIEW_ID = "com.developer.nefarious.zjoule.plugin.core.ui.ViewListener";

    /** Category label in the Show View dialog. */
    private static final String VIEW_CATEGORY = "ABAP Copilot";

    /** Tooltip of the login toolbar action. The action has no menu text. */
    private static final String LOGIN_TOOLTIP = "Click to log in to your BTP subaccount.";

    /** Preference page label contributed next to the page id. */
    private static final String PREFERENCE_PAGE = "ABAP Copilot";

    /** Driver for the workbench started by the UI harness. */
    private static SWTWorkbenchBot bot;

    /**
     * Creates the SWTBot driver and closes the welcome view when it is shown.
     */
    @BeforeAll
    public static void startBot() {
        bot = new SWTWorkbenchBot();
        closeWelcome();
    }

    /**
     * Closes dialogs left open by a failed test so the next test is not blocked.
     */
    @AfterEach
    public void closeDialogs() {
        closeShellIfPresent("Setup AI Provider");
        closeShellIfPresent("Preferences");
        closeShellIfPresent("Show View");
    }

    /**
     * Opens the chat view by id and checks the login toolbar action and the view menu.
     */
    @Test
    public void viewOpensByIdAndShowsActions() {
        SWTBotView view = openView();
        assertEquals(VIEW_ID, view.getViewReference().getId());
        assertTrue(isZJouleLabel(view.getViewReference().getPartName()));
        assertTrue(view.toolbarButton(LOGIN_TOOLTIP).isEnabled());
        assertMenuText(view, "Sessions");
        assertMenuText(view, "Preferences");
        assertMenuText(view, "Logout");
        capture("zjoule-view");
    }

    /**
     * Opens the chat view from Window &gt; Show View &gt; Other and cancels nothing.
     * <p>
     * The dialog node is matched without case, so both {@code zjoule} and {@code zJoule} pass.
     * </p>
     */
    @Test
    public void viewOpensFromShowViewDialog() {
        closeViewIfOpen();
        openShowViewDialog();
        SWTBotShell dialog = bot.shell("Show View");
        dialog.activate();
        dialog.bot().text().setText("zjoule");
        selectZJouleNode(dialog);
        dialog.bot().button("Open").click();
        bot.waitUntil(Conditions.shellCloses(dialog));
        SWTBotView view = openView();
        assertEquals(VIEW_ID, view.getViewReference().getId());
        assertTrue(isZJouleLabel(view.getViewReference().getPartName()));
    }

    /**
     * Opens the login wizard from the toolbar, checks both providers, and cancels.
     */
    @Test
    public void loginActionOpensProviderOptionsAndCancels() {
        SWTBotView view = openView();
        view.toolbarButton(LOGIN_TOOLTIP).click();
        SWTBotShell wizard = bot.shell("Setup AI Provider");
        wizard.activate();
        assertTrue(wizard.bot().radio("SAP AI Core").isVisible());
        assertTrue(wizard.bot().radio("Ollama (Local)").isVisible());
        assertTrue(wizard.bot().button("Select").isVisible());
        capture("login-wizard");
        wizard.bot().button("Cancel").click();
        bot.waitUntil(Conditions.shellCloses(wizard));
    }

    /**
     * Opens the preference page from the view menu and checks the instruction field.
     */
    @Test
    public void preferencePageShowsInstructionField() {
        SWTBotView view = openView();
        view.viewMenu("Preferences").click();
        SWTBotShell preferences = bot.shell("Preferences");
        preferences.activate();
        assertEquals(PREFERENCE_PAGE, preferences.bot().tree().getTreeItem(PREFERENCE_PAGE).getText());
        assertTrue(preferences.bot().label("Instructions:").isVisible());
        assertTrue(preferences.bot().label("Compaction threshold:").isVisible());
        assertTrue(preferences.bot().label("Messages to keep:").isVisible());
        capture("preferences");
        preferences.bot().button("Cancel").click();
        bot.waitUntil(Conditions.shellCloses(preferences));
    }

    /**
     * Shows the zJoule view by its contribution id and gives it focus.
     * <p>
     * {@code viewById} only sees a view that is already on the page, so this
     * asks the workbench to open that id first.
     * </p>
     *
     * @return the opened view.
     */
    private SWTBotView openView() {
        showViewById();
        SWTBotView view = bot.viewById(VIEW_ID);
        view.show();
        view.setFocus();
        return view;
    }

    /**
     * Opens the view through {@code IWorkbenchPage.showView}, on the UI thread.
     */
    private void showViewById() {
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                try {
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(VIEW_ID);
                } catch (PartInitException exception) {
                    throw new IllegalStateException(exception);
                }
            }
        });
    }

    /**
     * Closes the zJoule view when it is already open.
     */
    private void closeViewIfOpen() {
        for (SWTBotView view : bot.views()) {
            if (VIEW_ID.equals(view.getViewReference().getId())) {
                view.close();
            }
        }
    }

    /**
     * Opens Window &gt; Show View &gt; Other, whatever ellipsis the platform uses.
     */
    private void openShowViewDialog() {
        SWTBotMenu showView = bot.menu("Window").menu("Show View");
        String otherItem = null;
        for (String item : showView.menuItems()) {
            if (item != null && item.contains("Other")) {
                otherItem = item;
                break;
            }
        }
        if (otherItem == null) {
            throw new AssertionError("Window > Show View has no Other entry");
        }
        showView.menu(otherItem).click();
    }

    /**
     * Selects the zJoule node under the ABAP Copilot category.
     *
     * @param dialog the Show View shell.
     */
    private void selectZJouleNode(final SWTBotShell dialog) {
        SWTBotTreeItem category = dialog.bot().tree().expandNode(VIEW_CATEGORY);
        SWTBotTreeItem match = null;
        for (SWTBotTreeItem item : category.getItems()) {
            if (isZJouleLabel(item.getText())) {
                match = item;
                break;
            }
        }
        if (match == null) {
            throw new AssertionError("ABAP Copilot does not contain a zjoule or zJoule view");
        }
        match.select();
    }

    /**
     * Checks that a view-menu entry exists and then hides the menu.
     *
     * @param view the zJoule view.
     * @param label the menu item text.
     */
    private void assertMenuText(final SWTBotView view, final String label) {
        SWTBotMenu menu = view.viewMenu(label);
        assertEquals(label, menu.getText());
        menu.hide();
    }

    /**
     * Accepts the current view label and the capitalized label from the icon work.
     *
     * @param label the part name or tree label.
     * @return {@code true} when the label is {@code zjoule} or {@code zJoule}.
     */
    private boolean isZJouleLabel(final String label) {
        return label != null && "zjoule".equalsIgnoreCase(label.trim());
    }

    /**
     * Closes a dialog by title when it is open, without waiting for a missing shell.
     *
     * @param title the shell title.
     */
    private void closeShellIfPresent(final String title) {
        for (SWTBotShell shell : bot.shells()) {
            if (title.equals(shell.getText())) {
                shell.activate();
                shell.bot().button("Cancel").click();
                bot.waitUntil(Conditions.shellCloses(shell));
            }
        }
    }

    /**
     * Closes the welcome view when the platform still shows it.
     */
    private static void closeWelcome() {
        for (SWTBotView view : bot.views()) {
            if ("Welcome".equals(view.getTitle())) {
                view.close();
            }
        }
    }

    /**
     * Writes a PNG of the current display under {@code target/e2e-screenshots}.
     *
     * @param name the file name without an extension.
     */
    private void capture(final String name) {
        File file = new File("target/e2e-screenshots", name + ".png");
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        boolean captured = bot.captureScreenshot(file.getAbsolutePath());
        assertTrue(captured, file.getAbsolutePath());
        assertTrue(file.isFile());
        assertTrue(file.length() > 0);
    }
}
