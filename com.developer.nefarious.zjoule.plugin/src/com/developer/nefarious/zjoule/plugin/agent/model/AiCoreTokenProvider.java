package com.developer.nefarious.zjoule.plugin.agent.model;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.developer.nefarious.zjoule.plugin.auth.IAuthClient;
import com.developer.nefarious.zjoule.plugin.memory.SecureStore;
import com.developer.nefarious.zjoule.plugin.models.AccessToken;
import com.developer.nefarious.zjoule.plugin.models.ServiceKey;

/**
 * Caches an AI Core access token in memory until shortly before it expires.
 */
public final class AiCoreTokenProvider implements Supplier<String> {

    /** Refresh this many seconds before the token's stated expiry. */
    static final long EXPIRY_SKEW_SECONDS = 60L;

    private static final AtomicReference<AiCoreTokenProvider> ACTIVE = new AtomicReference<>();

    private final SecureStore store;

    private final IAuthClient authClient;

    private final Clock clock;

    private final Object lock = new Object();

    private AccessToken cached;

    /**
     * Creates the provider and registers it so logout can drop the cache.
     *
     * @param store the secure service-key store.
     * @param authClient the client-credentials client. Tokens are not saved.
     * @param clock the clock used for expiry. Tests inject a fixed clock.
     */
    public AiCoreTokenProvider(final SecureStore store, final IAuthClient authClient, final Clock clock) {
        this.store = store;
        this.authClient = authClient;
        this.clock = clock;
        ACTIVE.set(this);
    }

    /**
     * Drops the cache of the provider created for this plug-in, if one exists.
     */
    public static void invalidateActive() {
        AiCoreTokenProvider current = ACTIVE.get();
        if (current != null) {
            current.invalidate();
        }
    }

    /**
     * Returns a usable bearer token, refreshing it when the cache is stale.
     *
     * @return the access token string.
     */
    @Override
    public String get() {
        synchronized (lock) {
            if (cached != null && !expired(cached)) {
                return cached.getAccessToken();
            }
        }
        return refresh();
    }

    /**
     * Forgets the cached token so the next {@link #get()} requests a new one.
     */
    public void invalidate() {
        synchronized (lock) {
            cached = null;
        }
    }

    /**
     * Requests a new token when the cache is still stale.
     *
     * @return the access token string.
     */
    private String refresh() {
        synchronized (lock) {
            if (cached != null && !expired(cached)) {
                return cached.getAccessToken();
            }
            cached = request();
            return cached.getAccessToken();
        }
    }

    /**
     * Loads the service key and requests a token without persisting it.
     *
     * @return the new token.
     */
    private AccessToken request() {
        Optional<ServiceKey> key = store.serviceKey();
        if (key.isEmpty()) {
            throw new IllegalStateException("No service key");
        }
        try {
            return authClient.requestAccessToken(key.get());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not obtain an access token", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not obtain an access token", ex);
        }
    }

    /**
     * Returns whether the token should be replaced.
     * Lifetime is {@code expires_in} minus the skew, and never negative.
     *
     * @param token the cached token.
     * @return {@code true} when a new token is required.
     */
    private boolean expired(final AccessToken token) {
        long lifetime = Math.max(0L, token.getExpiresIn() - EXPIRY_SKEW_SECONDS);
        Instant received = receivedAt(token);
        return !clock.instant().isBefore(received.plusSeconds(lifetime));
    }

    /**
     * Uses the token's received time, or the current clock when it is missing.
     *
     * @param token the token.
     * @return the instant it was received.
     */
    private Instant receivedAt(final AccessToken token) {
        Date received = token.getReceivedAt();
        if (received == null) {
            return clock.instant();
        }
        return received.toInstant();
    }
}
