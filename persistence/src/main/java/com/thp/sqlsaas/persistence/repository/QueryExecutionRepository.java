package com.thp.sqlsaas.persistence.repository;

import com.thp.sqlsaas.persistence.entity.QueryExecution;
import com.thp.sqlsaas.persistence.entity.QueryExecution.QueryState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for QueryExecution entities.
 */
@Repository
public interface QueryExecutionRepository extends JpaRepository<QueryExecution, Long> {
    
    /**
     * Find query execution by trace ID.
     */
    Optional<QueryExecution> findByTraceId(String traceId);
    
    /**
     * Find all executions for a tenant.
     */
    List<QueryExecution> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    
    /**
     * Find all executions for a tenant and user.
     */
    List<QueryExecution> findByTenantIdAndUserIdOrderByCreatedAtDesc(
        String tenantId, 
        String userId
    );
    
    /**
     * Find executions by state.
     */
    List<QueryExecution> findByState(QueryState state);
    
    /**
     * Find executions created after a certain time.
     */
    List<QueryExecution> findByCreatedAtAfterOrderByCreatedAtDesc(Instant after);
}
