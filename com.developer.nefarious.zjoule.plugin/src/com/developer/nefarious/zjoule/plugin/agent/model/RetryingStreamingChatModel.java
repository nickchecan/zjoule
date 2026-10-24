package com.developer.nefarious.zjoule.plugin.agent.model;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * Retries one model call after HTTP 401 when no token has been emitted yet.
 * <p>
 * The retry stays inside this call. A second graph input would append the user
 * message again.
 */
public final class RetryingStreamingChatModel implements StreamingChatModel {

    private final Supplier<StreamingChatModel> models;

    private final Runnable invalidate;

    /**
     * Creates the wrapper.
     *
     * @param models supplies a fresh model for each attempt.
     * @param invalidate drops the cached access token before the retry.
     */
    public RetryingStreamingChatModel(final Supplier<StreamingChatModel> models, final Runnable invalidate) {
        this.models = models;
        this.invalidate = invalidate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
        AtomicBoolean emitted = new AtomicBoolean(false);
        AtomicBoolean retried = new AtomicBoolean(false);
        attempt(request, handler, emitted, retried);
    }

    /**
     * Runs one attempt and, on a clean 401, one more.
     *
     * @param request the chat request.
     * @param handler the outer handler.
     * @param emitted whether any partial token was delivered.
     * @param retried whether the single retry was already used.
     */
    private void attempt(final ChatRequest request, final StreamingChatResponseHandler handler,
            final AtomicBoolean emitted, final AtomicBoolean retried) {
        StreamingChatResponseHandler wrapped = wrap(request, handler, emitted, retried);
        try {
            models.get().doChat(request, wrapped);
        } catch (RuntimeException ex) {
            if (!retry(ex, emitted, retried)) {
                throw ex;
            }
            models.get().doChat(request, wrapped);
        }
    }

    /**
     * Forwards callbacks and retries an asynchronous 401 once.
     *
     * @param request the chat request.
     * @param handler the outer handler.
     * @param emitted whether any partial token was delivered.
     * @param retried whether the single retry was already used.
     * @return the wrapping handler.
     */
    private StreamingChatResponseHandler wrap(final ChatRequest request, final StreamingChatResponseHandler handler,
            final AtomicBoolean emitted, final AtomicBoolean retried) {
        return new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(final String token) {
                emitted.set(true);
                handler.onPartialResponse(token);
            }

            @Override
            public void onPartialResponse(final PartialResponse partial, final PartialResponseContext context) {
                emitted.set(true);
                handler.onPartialResponse(partial, context);
            }

            @Override
            public void onCompleteResponse(final ChatResponse response) {
                handler.onCompleteResponse(response);
            }

            @Override
            public void onError(final Throwable error) {
                if (retry(error, emitted, retried)) {
                    attempt(request, handler, emitted, retried);
                    return;
                }
                handler.onError(error);
            }
        };
    }

    /**
     * Invalidates the token and records that the one allowed retry is in use.
     *
     * @param error the failure.
     * @param emitted whether any partial token was delivered.
     * @param retried whether the single retry was already used.
     * @return {@code true} when the caller should try once more.
     */
    private boolean retry(final Throwable error, final AtomicBoolean emitted, final AtomicBoolean retried) {
        if (retried.get() || emitted.get() || !HttpFailures.isUnauthorized(error)) {
            return false;
        }
        retried.set(true);
        invalidate.run();
        return true;
    }
}
