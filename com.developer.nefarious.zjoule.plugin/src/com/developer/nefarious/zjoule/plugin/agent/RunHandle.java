package com.developer.nefarious.zjoule.plugin.agent;

/**
 * Controls one running chat turn.
 */
public interface RunHandle {

    /**
     * Stops the graph stream and the HTTP stream.
     */
    void cancel();

    /**
     * Returns whether the background job has finished.
     *
     * @return {@code true} after completion, cancellation, or failure.
     */
    boolean isDone();

    /**
     * Returns whether {@link #cancel()} was requested.
     *
     * @return {@code true} when the turn was cancelled.
     */
    boolean wasCancelled();
}
