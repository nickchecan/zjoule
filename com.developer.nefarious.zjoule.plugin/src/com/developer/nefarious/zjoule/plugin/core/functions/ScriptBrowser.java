package com.developer.nefarious.zjoule.plugin.core.functions;

/**
 * Executes a script in the chat view. Tests substitute a stand-in for the SWT browser.
 */
public interface ScriptBrowser {

    /**
     * Returns whether the browser has been disposed.
     *
     * @return {@code true} when scripts must not be executed.
     */
    boolean isDisposed();

    /**
     * Executes a script.
     *
     * @param script the script.
     */
    void execute(final String script);
}
