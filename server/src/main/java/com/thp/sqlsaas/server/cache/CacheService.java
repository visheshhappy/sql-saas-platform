package com.thp.sqlsaas.server.cache;

import com.thp.sqlsaas.server.model.QueryExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache Service - Handles caching of query results.
 * 
 * In production, this would use Redis or similar distributed cache.
 * For now, using in-memory cache for simplicity.
 */
@Service
public class CacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
    // Simple in-memory cache
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    /**
     * Get cached result for a query.
     * 
     * @param cacheKey the cache key
     * @param maxStalenessMs maximum allowed staleness in milliseconds
     * @return cached result if valid, null otherwise
     */
    public QueryExecutionResult get(String cacheKey, long maxStalenessMs) {
        CacheEntry entry = cache.get(cacheKey);
        
        if (entry == null) {
            logger.debug("Cache miss for key: {}", cacheKey);
            return null;
        }
        
        long age = System.currentTimeMillis() - entry.timestamp;
        
        if (age > maxStalenessMs) {
            logger.debug("Cache expired for key: {} (age: {}ms, max: {}ms)", 
                        cacheKey, age, maxStalenessMs);
            cache.remove(cacheKey);
            return null;
        }
        
        logger.debug("Cache hit for key: {} (age: {}ms)", cacheKey, age);
        return entry.result;
    }
    
    /**
     * Put a result in the cache.
     */
    public void put(String cacheKey, QueryExecutionResult result) {
        cache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis()));
        logger.debug("Cached result for key: {}", cacheKey);
    }
    
    /**
     * Invalidate cache for a specific key.
     */
    public void invalidate(String cacheKey) {
        cache.remove(cacheKey);
        logger.debug("Invalidated cache for key: {}", cacheKey);
    }
    
    /**
     * Clear all cache entries.
     */
    public void clear() {
        cache.clear();
        logger.info("Cleared all cache entries");
    }
    
    /**
     * Generate a cache key from query parameters.
     */
    public static String generateCacheKey(
            String tenantId,
            String userId,
            String sql) {
        return String.format("%s:%s:%s", tenantId, userId, sql.hashCode());
    }
    
    /**
     * Cache entry holder.
     */
    private record CacheEntry(
        QueryExecutionResult result,
        long timestamp
    ) {}
}
