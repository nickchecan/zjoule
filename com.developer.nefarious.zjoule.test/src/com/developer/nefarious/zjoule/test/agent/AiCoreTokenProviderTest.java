package com.developer.nefarious.zjoule.test.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.junit.jupiter.api.Test;

import com.developer.nefarious.zjoule.plugin.agent.model.AiCoreTokenProvider;
import com.developer.nefarious.zjoule.plugin.auth.IAuthClient;
import com.developer.nefarious.zjoule.plugin.memory.SecureStore;
import com.developer.nefarious.zjoule.plugin.models.AccessToken;

public class AiCoreTokenProviderTest {

    private static final long EXPIRES_IN = 120L;

    private static final long STILL_VALID_SECONDS = 59L;

    private static final long EXPIRED_STEP_SECONDS = 1L;

    private static final int REQUESTS_AFTER_INVALIDATE = 3;

    private static final String SERVICE_KEY_JSON = "{"
            + "\"serviceurls\": {\"AI_API_URL\": \"https://api.example\"}, "
            + "\"clientid\": \"client\", "
            + "\"clientsecret\": \"secret\", "
            + "\"url\": \"https://auth.example\""
            + "}";

    @Test
    public void shouldCacheUntilTheSkewedExpiryAndRefreshAfterInvalidate() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AtomicInteger calls = new AtomicInteger();
        IAuthClient auth = mock(IAuthClient.class);
        when(auth.requestAccessToken(any())).thenAnswer(invocation -> token(clock, calls.incrementAndGet()));
        AiCoreTokenProvider provider = new AiCoreTokenProvider(store(), auth, clock);

        assertEquals("t1", provider.get());
        assertEquals("t1", provider.get());
        assertEquals(1, calls.get());

        clock.advanceSeconds(STILL_VALID_SECONDS);
        assertEquals("t1", provider.get());
        assertEquals(1, calls.get());

        clock.advanceSeconds(EXPIRED_STEP_SECONDS);
        assertEquals("t2", provider.get());
        assertEquals(2, calls.get());

        provider.invalidate();
        assertEquals("t" + REQUESTS_AFTER_INVALIDATE, provider.get());
        assertEquals(REQUESTS_AFTER_INVALIDATE, calls.get());
    }

    private static AccessToken token(final MutableClock clock, final int sequence) {
        AccessToken token = new AccessToken();
        token.setAccessToken("t" + sequence);
        token.setExpiresIn(EXPIRES_IN);
        token.setReceivedAt(Date.from(clock.instant()));
        return token;
    }

    private static SecureStore store() throws Exception {
        Map<String, String> values = new HashMap<>();
        values.put(SecureStore.KEY, SERVICE_KEY_JSON);
        ISecurePreferences node = mock(ISecurePreferences.class);
        when(node.get(any(), nullable(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String fallback = invocation.getArgument(1);
            return values.getOrDefault(key, fallback);
        });
        return new SecureStore(node);
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(final Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        private void advanceSeconds(final long seconds) {
            now = now.plusSeconds(seconds);
        }
    }
}
