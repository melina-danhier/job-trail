package com.melina.jobtrail.service;

import com.melina.jobtrail.exception.AiRateLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobMatchRateLimiter {
    private final int requestsPerWindow;
    private final Duration window;
    private final Clock clock;
    private final Map<String, RequestWindow> requests = new ConcurrentHashMap<>();

    @Autowired
    public JobMatchRateLimiter(
            @Value("${job-match.rate-limit.requests:5}") int requestsPerWindow,
            @Value("${job-match.rate-limit.window:PT1M}") Duration window
    ) {
        this(requestsPerWindow, window, Clock.systemUTC());
    }

    JobMatchRateLimiter(int requestsPerWindow, Duration window, Clock clock) {
        if (requestsPerWindow < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Invalid job matching rate limit configuration");
        }
        this.requestsPerWindow = requestsPerWindow;
        this.window = window;
        this.clock = clock;
    }

    public void checkAllowed(String userId) {
        Instant now = clock.instant();
        requests.compute(userId, (ignored, existingWindow) -> {
            RequestWindow requestWindow = existingWindow == null ? new RequestWindow() : existingWindow;
            removeExpired(requestWindow.timestamps, now.minus(window));
            if (requestWindow.timestamps.size() >= requestsPerWindow) {
                throw new AiRateLimitException();
            }
            requestWindow.timestamps.addLast(now);
            requestWindow.lastAccess = now;
            return requestWindow;
        });
    }

    @Scheduled(fixedDelayString = "${job-match.rate-limit.cleanup-interval:PT5M}")
    void cleanupExpiredEntries() {
        Instant cutoff = clock.instant().minus(window);
        requests.keySet().forEach(userId ->
                requests.computeIfPresent(userId, (ignored, requestWindow) -> {
                    removeExpired(requestWindow.timestamps, cutoff);
                    if (requestWindow.timestamps.isEmpty() && !requestWindow.lastAccess.isAfter(cutoff)) {
                        return null;
                    }
                    return requestWindow;
                })
        );
    }

    int trackedUsers() {
        return requests.size();
    }

    private void removeExpired(Deque<Instant> timestamps, Instant cutoff) {
        while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(cutoff)) {
            timestamps.removeFirst();
        }
    }

    private static final class RequestWindow {
        private final Deque<Instant> timestamps = new ArrayDeque<>();
        private Instant lastAccess = Instant.MIN;
    }
}
