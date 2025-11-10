package com.thp.sqlsaas.server.orchestrator;

/**
 * Result of a rate limit check.
 */
public class RateLimitDecision {
    private final boolean allowed;
    private final int remainingRequests;
    private final Long retryAfterSeconds;
    private final String message;
    
    public RateLimitDecision(
            boolean allowed, 
            int remainingRequests, 
            Long retryAfterSeconds, 
            String message) {
        this.allowed = allowed;
        this.remainingRequests = remainingRequests;
        this.retryAfterSeconds = retryAfterSeconds;
        this.message = message;
    }
    
    public boolean isAllowed() {
        return allowed;
    }
    
    public int getRemainingRequests() {
        return remainingRequests;
    }
    
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
    
    public String getMessage() {
        return message;
    }
}
