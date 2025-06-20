// BaseException.java
package com.company.receipt.exception;

public abstract class BaseException extends RuntimeException {
    private final String errorCode;
    
    public BaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BaseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
