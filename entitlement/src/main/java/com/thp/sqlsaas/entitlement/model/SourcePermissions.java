package com.thp.sqlsaas.entitlement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Permissions extracted from source system (e.g., GitHub, Salesforce).
 * Represents what the user can access in the source system itself.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourcePermissions {
    
    // Tables/objects the user can read
    @Builder.Default
    private Set<String> readableTables = new HashSet<>();
    
    // Columns/fields accessible per table
    @Builder.Default
    private Map<String, Set<String>> tableColumns = new HashMap<>();
    
    // Source-native row filters (e.g., Salesforce sharing rules)
    @Builder.Default
    private Map<String, String> nativeRowFilters = new HashMap<>();
    
    // Any other source-specific metadata
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    public boolean canReadTable(String tableName) {
        return readableTables.contains(tableName);
    }
    
    public boolean canReadColumn(String tableName, String columnName) {
        Set<String> columns = tableColumns.get(tableName);
        return columns != null && columns.contains(columnName);
    }
}
