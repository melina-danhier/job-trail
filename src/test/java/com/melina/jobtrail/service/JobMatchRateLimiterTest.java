package com.melina.jobtrail.service;

import com.melina.jobtrail.exception.AiRateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobMatchRateLimiterTest {

    @Test
    void rejectsRequestsAboveLimitForSameUser() {
        JobMatchRateLimiter limiter = new JobMatchRateLimiter(
                2, Duration.ofMinutes(1), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        limiter.checkAllowed("user@example.com");
        limiter.checkAllowed("user@example.com");

        assertThrows(AiRateLimitException.class,
                () -> limiter.checkAllowed("user@example.com"));
        assertDoesNotThrow(() -> limiter.checkAllowed("other@example.com"));
    }

    @Test
    void allowsRequestsAgainAfterWindow() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(start);
        JobMatchRateLimiter limiter = new JobMatchRateLimiter(1, Duration.ofMinutes(1), clock);
        limiter.checkAllowed("user@example.com");
        assertThrows(AiRateLimitException.class,
                () -> limiter.checkAllowed("user@example.com"));

        clock.advance(Duration.ofSeconds(61));
        assertDoesNotThrow(() -> limiter.checkAllowed("user@example.com"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
