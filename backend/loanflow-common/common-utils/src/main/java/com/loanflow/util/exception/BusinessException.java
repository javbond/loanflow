package com.loanflow.util.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final Object details;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(String errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    // Common business error factory methods
    public static BusinessException invalidStatus(String currentStatus, String requiredStatus) {
        return new BusinessException(
                "INVALID_STATUS",
                String.format("Invalid status transition. Current: %s, Required: %s",
                        currentStatus, requiredStatus));
    }

    public static BusinessException insufficientPermission(String action) {
        return new BusinessException(
                "INSUFFICIENT_PERMISSION",
                String.format("Insufficient permission to perform: %s", action));
    }

    public static BusinessException limitExceeded(String limitType, Number limit) {
        return new BusinessException(
                "LIMIT_EXCEEDED",
                String.format("%s limit exceeded. Maximum allowed: %s", limitType, limit));
    }

    public static BusinessException invalidOperation(String reason) {
        return new BusinessException("INVALID_OPERATION", reason);
    }
}
