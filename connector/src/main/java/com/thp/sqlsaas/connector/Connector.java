package com.thp.sqlsaas.connector;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Connector {

    ConnectResult connect(ConnectRequest req)
        throws ConnectorException;

    RowPage executeScan(ExecuteScanRequest req)
        throws ConnectorException;

    void close();

    public record ConnectRequest(String tenantId, Map<String, String> config){}

    public record ConnectResult(
        CapabilityDescriptor capabilities,      // resources, columns, pushdowns
        List<String> repos,                     // owner/name allowlist
        Map<String, Object> sessionContext      // tokens for real; empty for mock
    ){}

    public record CapabilityDescriptor(
        Set<String> resources,
        Map<String, Set<String>> columns,
        Map<String, Set<String>> pushdownableFields
    ){}

    public record ExecuteScanRequest(
        String tenantId,
        String resource,                 // "issues", "pulls"
        List<String> columns,            // projection
        List<Predicate> predicates,      // repo IN, state =, updated_at >=, etc.
        Integer limit,
        String pageToken,
        Long maxStalenessMs
    ){}

    public record Predicate(String field, String op, Object value) {}
    public record RowPage(List<Map<String,Object>> rows, String nextPageToken, long freshnessMs){}
}
