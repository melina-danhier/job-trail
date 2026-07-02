package com.melina.jobtrail.service;

import com.melina.jobtrail.exception.AiRateLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    private final Map<String, Deque<Instant>> requests = new ConcurrentHashMap<>();

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
        Deque<Instant> userRequests = requests.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (userRequests) {
            Instant cutoff = now.minus(window);
            while (!userRequests.isEmpty() && !userRequests.peekFirst().isAfter(cutoff)) {
                userRequests.removeFirst();
            }
            if (userRequests.size() >= requestsPerWindow) {
                throw new AiRateLimitException();
            }
            userRequests.addLast(now);
        }
    }
}
