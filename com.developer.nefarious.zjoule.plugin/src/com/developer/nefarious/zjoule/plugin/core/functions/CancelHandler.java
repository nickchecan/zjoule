package com.developer.nefarious.zjoule.plugin.core.functions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

/**
 * Browser function {@code cancelAIResponse}. Stops the running chat turn.
 */
public class CancelHandler extends BrowserFunction {

    /**
     * Factory method to create a new {@code CancelHandler}.
     *
     * @param browser the browser associated with this function.
     * @param name the JavaScript function name.
     * @return a new handler.
     */
    public static CancelHandler create(final Browser browser, final String name) {
        return new CancelHandler(browser, name);
    }

    /**
     * Constructs a new {@code CancelHandler}.
     *
     * @param browser the browser associated with this function.
     * @param name the JavaScript function name.
     */
    public CancelHandler(final Browser browser, final String name) {
        super(browser, name);
    }

    /**
     * Cancels the turn started by {@link PromptHandler}.
     *
     * @param arguments ignored.
     * @return always {@code null}.
     */
    @Override
    public Object function(final Object[] arguments) {
        PromptHandler.cancelActiveResponse();
        return null;
    }
}
