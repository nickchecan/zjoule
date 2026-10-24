package com.developer.nefarious.zjoule.plugin.core.functions;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Display;

import com.developer.nefarious.zjoule.plugin.agent.ChatSessionService;
import com.developer.nefarious.zjoule.plugin.agent.RunHandle;
import com.developer.nefarious.zjoule.plugin.agent.persistence.SessionInfo;
import com.developer.nefarious.zjoule.plugin.chat.utils.EditorContentReader;
import com.developer.nefarious.zjoule.plugin.core.Activator;

/**
 * Handles user prompts from the browser and streams the model response.
 */
public class PromptHandler extends BrowserFunction {

    /** The turn started by the latest prompt. */
    private static final AtomicReference<RunHandle> ACTIVE = new AtomicReference<>();

    private final Browser browser;

    /**
     * Factory method to create a new {@code PromptHandler} instance.
     *
     * @param browser the {@link Browser} instance associated with this function.
     * @param name the name of the JavaScript function exposed to the browser.
     * @return a new {@code PromptHandler} instance.
     */
    public static PromptHandler create(final Browser browser, final String name) {
        return new PromptHandler(browser, name);
    }

    /**
     * Cancels the turn started by the latest prompt, if it is still running.
     */
    public static void cancelActiveResponse() {
        RunHandle handle = ACTIVE.get();
        if (handle != null) {
            handle.cancel();
        }
    }

    /**
     * Constructs a new {@code PromptHandler} instance.
     *
     * @param browser the {@link Browser} instance associated with this function.
     * @param name the name of the JavaScript function exposed to the browser.
     */
    public PromptHandler(final Browser browser, final String name) {
        super(browser, name);
        this.browser = browser;
    }

    /**
     * Sends the prompt to the active session.
     *
     * @param arguments the JavaScript arguments. The first element is the prompt.
     * @return always {@code null}. The answer arrives through the stream listener.
     */
    @Override
    public Object function(final Object[] arguments) {
        if (arguments == null || arguments.length == 0 || arguments[0] == null) {
            throw new IllegalArgumentException("A prompt is required");
        }
        String userPrompt = arguments[0].toString();
        String editorContent = EditorContentReader.readActiveEditorContent();
        ChatSessionService sessions = Activator.getDefault().chatSessions();
        String threadId = activeThread(sessions);
        BrowserStreamBridge bridge = new BrowserStreamBridge(browser, Display.getDefault());
        ACTIVE.set(sessions.send(threadId, userPrompt, editorContent, bridge));
        return null;
    }

    /**
     * Uses the active session, creating one when the view has none.
     *
     * @param sessions the session service.
     * @return the thread id.
     */
    private static String activeThread(final ChatSessionService sessions) {
        Optional<SessionInfo> active = sessions.active();
        if (active.isPresent()) {
            return active.get().threadId();
        }
        return sessions.newSession().threadId();
    }
}
