package com.developer.nefarious.zjoule.plugin.agent.model;

import dev.langchain4j.exception.HttpException;

/**
 * Recognises HTTP failures that the chat runtime can recover from.
 */
final class HttpFailures {

    /** Status that means the cached AI Core token was rejected. */
    static final int UNAUTHORIZED = 401;

    private HttpFailures() {
    }

    /**
     * Returns whether the failure, or one of its causes, is HTTP 401.
     *
     * @param error the failure.
     * @return {@code true} for an unauthorized response.
     */
    static boolean isUnauthorized(final Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof HttpException http && http.statusCode() == UNAUTHORIZED) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
