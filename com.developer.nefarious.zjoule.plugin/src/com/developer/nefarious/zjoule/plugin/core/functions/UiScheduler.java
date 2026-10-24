package com.developer.nefarious.zjoule.plugin.core.functions;

/**
 * Hops work onto the UI thread. Tests substitute a stand-in for {@code Display}.
 */
public interface UiScheduler {

    /**
     * Runs work on the UI thread.
     *
     * @param runnable the work.
     */
    void asyncExec(final Runnable runnable);

    /**
     * Runs work on the UI thread after a delay.
     *
     * @param milliseconds the delay.
     * @param runnable the work.
     */
    void timerExec(final int milliseconds, final Runnable runnable);
}
