package com.developer.nefarious.zjoule.plugin.agent;

/**
 * Receives chat events on the background job thread.
 * Implementations that touch the UI must hop to the UI thread themselves.
 */
public interface ChatStreamListener {

    /**
     * Called when a turn starts.
     */
    void onStart();

    /**
     * Called for one streaming chunk.
     *
     * @param chunk the text fragment.
     */
    void onToken(final String chunk);

    /**
     * Called when the model finishes a normal turn.
     *
     * @param fullText the assistant text stored in the checkpoint.
     */
    void onComplete(final String fullText);

    /**
     * Called when a later tools node pauses for approval.
     *
     * @param toolSummaryJson a short description of the pending call.
     */
    void onApprovalRequired(final String toolSummaryJson);

    /**
     * Called when the user or the Progress view cancels the turn.
     */
    void onCancelled();

    /**
     * Called when the turn fails.
     *
     * @param error the failure.
     */
    void onError(final Throwable error);
}
