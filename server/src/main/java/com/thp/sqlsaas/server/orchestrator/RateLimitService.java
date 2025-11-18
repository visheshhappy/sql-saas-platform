package com.thp.sqlsaas.server.orchestrator;

import com.thp.sqlsaas.connector.ConnectorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Service - Token bucket implementation.
 * Controls rate limits per tenant, per user, per connector.
 */
@Service
public class RateLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    
    // Simple in-memory token bucket
    // Key format: "tenantId:userId:connectorType"
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    
    // Configuration per connector type
    private final Map<ConnectorType, RateLimitConfig> configs = Map.of(
        ConnectorType.GITHUB, new RateLimitConfig(1, 1), // 100 requests per 60 seconds
        ConnectorType.JIRA, new RateLimitConfig(100, 60)     // 100 requests per 60 seconds
    );
    
    /**
     * Check if the request is allowed based on rate limits.
     */
    public RateLimitDecision checkRateLimit(
            String tenantId, 
            String userId, 
            ConnectorType connectorType) {
        
        String key = buildKey(tenantId, userId, connectorType);
        RateLimitConfig config = configs.getOrDefault(
            connectorType, 
            new RateLimitConfig(100, 60)
        );
        
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(config));
        
        if (bucket.tryConsume()) {
            int remaining = bucket.availableTokens();
            logger.debug("Rate limit OK - key: {}, remaining: {}", key, remaining);
            return new RateLimitDecision(true, remaining, null, null);
        } else {
            long retryAfter = bucket.getRetryAfterSeconds();
            String message = String.format(
                "Rate limit exceeded for %s. Please retry after %d seconds.",
                connectorType, retryAfter
            );
            logger.warn("Rate limit exceeded - key: {}, retryAfter: {}s", key, retryAfter);
            return new RateLimitDecision(false, 0, retryAfter, message);
        }
    }
    
    private String buildKey(String tenantId, String userId, ConnectorType connectorType) {
        return String.format("%s:%s:%s", tenantId, userId, connectorType);
    }
    
    /**
     * Token bucket implementation for rate limiting.
     */
    private static class TokenBucket {
        private final int capacity;
        private final long refillPeriodSeconds;
        private final AtomicInteger tokens;
        private volatile long lastRefillTime;
        
        public TokenBucket(RateLimitConfig config) {
            this.capacity = config.requestsPerPeriod;
            this.refillPeriodSeconds = config.periodSeconds;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = System.currentTimeMillis();
        }
        
        public synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }
        
        public int availableTokens() {
            refill();
            return tokens.get();
        }
        
        public long getRetryAfterSeconds() {
            long elapsed = (System.currentTimeMillis() - lastRefillTime) / 1000;
            return Math.max(0, refillPeriodSeconds - elapsed);
        }
        
        private synchronized void refill() {
            long now = System.currentTimeMillis();
            long elapsed = (now - lastRefillTime) / 1000;
            
            if (elapsed >= refillPeriodSeconds) {
                tokens.set(capacity);
                lastRefillTime = now;
            }
        }
    }
    
    /**
     * Rate limit configuration per connector.
     */
    private record RateLimitConfig(int requestsPerPeriod, long periodSeconds) {}
}
