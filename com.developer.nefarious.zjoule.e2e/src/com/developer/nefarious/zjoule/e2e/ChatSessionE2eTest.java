package com.developer.nefarious.zjoule.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit5.SWTBotJunit5Extension;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.developer.nefarious.zjoule.plugin.agent.model.ScriptedChat;

/**
 * Drives the chat view through the scripted model. No network.
 */
@ExtendWith(SWTBotJunit5Extension.class)
public class ChatSessionE2eTest {

    private static final String VIEW_ID = "com.developer.nefarious.zjoule.plugin.core.ui.ViewListener";

    private static SWTWorkbenchBot bot;

    /**
     * Creates the SWTBot driver.
     */
    @BeforeAll
    public static void startBot() {
        bot = new SWTWorkbenchBot();
    }

    /**
     * Opens the chat and waits until the scripted login has shown it.
     */
    @BeforeEach
    public void openChat() {
        showView();
        bot.viewById(VIEW_ID).show();
        waitUntilLoggedIn();
    }

    /**
     * Drops a held stream so a failed test cannot block the next one.
     */
    @AfterEach
    public void releaseHold() {
        System.setProperty(ScriptedChat.HOLD, "false");
    }

    /**
     * Sends one prompt, checks the streamed reply, and checks it after the view is reopened.
     */
    @Test
    public void streamedReplySurvivesReopen() {
        newChat();
        send("stream-one");
        waitForText("stream-one");
        waitForText(ScriptedChat.REPLY);
        reopen();
        waitForText("stream-one");
        waitForText(ScriptedChat.REPLY);
    }

    /**
     * Opens a second session, switches back, and deletes the session on screen.
     */
    @Test
    public void switchingSessionsAndDeleteCurrent() {
        newChat();
        send("session-alpha");
        waitForText("session-alpha");
        newChat();
        send("session-beta");
        waitForText("session-beta");
        menu("session-alpha");
        waitForText("session-alpha");
        assertFalse(chatText().contains("session-beta"));
        menu("Delete current");
        waitForAbsent("session-alpha");
        List<String> items = bot.viewById(VIEW_ID).viewMenu("Sessions").menuItems();
        assertFalse(items.contains("session-alpha"), items.toString());
    }

    /**
     * Stops a held reply. The user message stays after the view is reopened.
     */
    @Test
    public void stopLeavesTheUserMessage() {
        System.setProperty(ScriptedChat.HOLD, "true");
        System.setProperty(ScriptedChat.ARMED, "false");
        newChat();
        send("stop-please");
        waitUntilArmed();
        clickStop();
        waitForText("(cancelled)");
        assertTrue(chatText().contains("stop-please"));
        reopen();
        waitForText("stop-please");
        assertFalse(chatText().contains(ScriptedChat.REPLY));
    }

    /**
     * Three short turns cross the scripted threshold. Reopening shows only the kept tail.
     */
    @Test
    public void compactionDropsOlderMessagesAfterReopen() {
        say("drop-first");
        say("drop-second");
        say("keep-third");
        reopen();
        waitForText("keep-third");
        assertFalse(chatText().contains("drop-first"), chatText());
        assertFalse(chatText().contains("drop-second"), chatText());
    }

    /**
     * Opens the view by id.
     */
    private static void showView() {
        UIThreadRunnable.syncExec(() -> {
            try {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(VIEW_ID);
            } catch (PartInitException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    /**
     * Closes and opens the view so history is rendered from the checkpoint.
     */
    private void reopen() {
        bot.viewById(VIEW_ID).close();
        showView();
        bot.viewById(VIEW_ID).show();
        waitUntilLoggedIn();
    }

    /**
     * Starts an empty session from the view menu.
     */
    private void newChat() {
        menu("New chat");
    }

    /**
     * Clicks one Sessions item.
     *
     * @param label the item text.
     */
    private void menu(final String label) {
        bot.viewById(VIEW_ID).viewMenu("Sessions").menu(label).click();
    }

    /**
     * Sends one prompt and waits until that turn has finished.
     *
     * @param text the prompt.
     */
    private void say(final String text) {
        send(text);
        waitForText(text);
        waitForText(ScriptedChat.REPLY);
        waitUntilIdle();
    }

    /**
     * Submits one user message through the page script.
     *
     * @param text the prompt.
     */
    private void send(final String text) {
        Browser browser = browser();
        String script = "document.getElementById('userInput').value = '" + text + "'; sendMessage();";
        Boolean ran = UIThreadRunnable.syncExec(() -> Boolean.valueOf(browser.execute(script)));
        if (!Boolean.TRUE.equals(ran)) {
            throw new IllegalStateException("sendMessage did not run. Chat: " + chatText());
        }
    }

    /**
     * Clicks Stop.
     */
    private void clickStop() {
        Browser browser = browser();
        UIThreadRunnable.syncExec(() -> browser.execute("document.getElementById('stop').click();"));
    }

    /**
     * Waits until the scripted login has shown the chat.
     */
    private void waitUntilLoggedIn() {
        bot.waitUntil(new DefaultCondition() {
            @Override
            public boolean test() {
                return Boolean.TRUE.equals(eval(
                        "var box = document.querySelector('.chat-container'); return !!(box && box.style.display !== 'none');"));
            }

            @Override
            public String getFailureMessage() {
                return "Chat was not shown";
            }
        });
    }

    /**
     * Waits until the held stream has published a cancel handle.
     */
    private void waitUntilArmed() {
        bot.waitUntil(new DefaultCondition() {
            @Override
            public boolean test() {
                return "true".equals(System.getProperty(ScriptedChat.ARMED));
            }

            @Override
            public String getFailureMessage() {
                return "The stream did not publish a cancel handle";
            }
        });
    }

    /**
     * Waits until the chat transcript contains the text.
     *
     * @param expected the text.
     */
    private void waitForText(final String expected) {
        bot.waitUntil(new DefaultCondition() {
            @Override
            public boolean test() {
                return chatText().contains(expected);
            }

            @Override
            public String getFailureMessage() {
                return "Chat did not contain " + expected + " but was: " + chatText();
            }
        });
    }

    /**
     * Waits until Stop is disabled, which is the idle chat.
     */
    private void waitUntilIdle() {
        bot.waitUntil(new DefaultCondition() {
            @Override
            public boolean test() {
                return Boolean.TRUE.equals(eval("return document.getElementById('stop').disabled;"));
            }

            @Override
            public String getFailureMessage() {
                return "The turn did not finish. Chat: " + chatText();
            }
        });
    }

    /**
     * Waits until the chat transcript no longer contains the text.
     *
     * @param unexpected the text.
     */
    private void waitForAbsent(final String unexpected) {
        bot.waitUntil(new DefaultCondition() {
            @Override
            public boolean test() {
                return !chatText().contains(unexpected);
            }

            @Override
            public String getFailureMessage() {
                return "Chat still contains " + unexpected;
            }
        });
    }

    /**
     * Returns the visible chat transcript.
     *
     * @return the chat box text.
     */
    private String chatText() {
        Object value = eval("return document.getElementById('chatBox') ? document.getElementById('chatBox').innerText : '';");
        return value == null ? "" : value.toString();
    }

    /**
     * Evaluates a script in the chat browser.
     *
     * @param script the script.
     * @return the value.
     */
    private Object eval(final String script) {
        Browser browser = browser();
        return UIThreadRunnable.syncExec(() -> browser.evaluate(script));
    }

    /**
     * Finds the chat browser inside the view.
     *
     * @return the browser.
     */
    private Browser browser() {
        Widget widget = bot.viewById(VIEW_ID).getWidget();
        Browser browser = UIThreadRunnable.syncExec(() -> findBrowser(widget));
        if (browser == null) {
            throw new IllegalStateException("Chat view has no browser");
        }
        return browser;
    }

    /**
     * Walks the view controls for the browser.
     *
     * @param widget the current control.
     * @return the browser, or {@code null}.
     */
    private Browser findBrowser(final Widget widget) {
        if (widget instanceof Browser browser) {
            return browser;
        }
        if (widget instanceof Composite composite) {
            for (Control child : composite.getChildren()) {
                Browser found = findBrowser(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
