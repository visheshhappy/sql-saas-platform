package com.thp.sqlsaas.connector;

/**
 * Base exception for all connector-related errors.
 */
public class ConnectorException extends Exception {
    
    private final ErrorCode errorCode;
    private final String connectorId;
    
    public ConnectorException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.connectorId = null;
    }
    
    public ConnectorException(ErrorCode errorCode, String message, String connectorId) {
        super(message);
        this.errorCode = errorCode;
        this.connectorId = connectorId;
    }
    
    public ConnectorException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.connectorId = null;
    }
    
    public ConnectorException(ErrorCode errorCode, String message, String connectorId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.connectorId = connectorId;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getConnectorId() {
        return connectorId;
    }
    
    /**
     * Standard error codes for connectors
     */
    public enum ErrorCode {
        AUTHENTICATION_FAILED,
        AUTHORIZATION_FAILED,
        RATE_LIMIT_EXHAUSTED,
        SOURCE_TIMEOUT,
        SOURCE_UNAVAILABLE,
        INVALID_REQUEST,
        RESOURCE_NOT_FOUND,
        STALE_DATA,
        CONFIGURATION_ERROR,
        UNKNOWN_ERROR
    }
}
