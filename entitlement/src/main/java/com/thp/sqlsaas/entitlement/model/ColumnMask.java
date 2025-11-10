package com.thp.sqlsaas.entitlement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Defines how to mask/transform a column value.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMask {
    
    private MaskType maskType;
    private String policyId;
    
    public enum MaskType {
        FULL,           // ****
        PARTIAL,        // Show last 4: ***-**-1234
        HASH,           // SHA256 hash
        REDACT,         // [REDACTED]
        NULL            // Replace with NULL
    }
    
    /**
     * Apply masking to a value
     */
    public Object mask(Object value) {
        if (value == null) {
            return null;
        }
        
        String strValue = String.valueOf(value);
        
        return switch (maskType) {
            case FULL -> "****";
            case PARTIAL -> maskPartial(strValue);
            case HASH -> hashValue(strValue);
            case REDACT -> "[REDACTED]";
            case NULL -> null;
        };
    }
    
    private String maskPartial(String value) {
        if (value.length() <= 4) {
            return "****";
        }
        String last4 = value.substring(value.length() - 4);
        return "***-***-" + last4;
    }
    
    private String hashValue(String value) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes());
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "[HASH_ERROR]";
        }
    }
}
