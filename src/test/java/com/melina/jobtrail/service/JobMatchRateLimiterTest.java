package com.melina.jobtrail.service;

import com.melina.jobtrail.exception.AiRateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void cleanupRemovesInactiveUsersAfterWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        JobMatchRateLimiter limiter = new JobMatchRateLimiter(2, Duration.ofMinutes(1), clock);

        limiter.checkAllowed("first@example.com");
        limiter.checkAllowed("second@example.com");
        assertEquals(2, limiter.trackedUsers());

        clock.advance(Duration.ofMinutes(2));
        limiter.cleanupExpiredEntries();

        assertEquals(0, limiter.trackedUsers());
    }

    @Test
    void concurrentRequestsCannotExceedLimit() throws Exception {
        JobMatchRateLimiter limiter = new JobMatchRateLimiter(
                5, Duration.ofMinutes(1), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        );
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(10)) {
            List<Future<Boolean>> results = java.util.stream.IntStream.range(0, 20)
                    .mapToObj(ignored -> executor.submit(() -> {
                        start.await();
                        try {
                            limiter.checkAllowed("user@example.com");
                            return true;
                        } catch (AiRateLimitException ex) {
                            return false;
                        }
                    }))
                    .toList();

            start.countDown();
            long accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    accepted++;
                }
            }
            assertEquals(5, accepted);
        }
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
