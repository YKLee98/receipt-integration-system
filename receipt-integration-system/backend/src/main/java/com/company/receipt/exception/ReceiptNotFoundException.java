// ReceiptNotFoundException.java
package com.company.receipt.exception;

public class ReceiptNotFoundException extends BaseException {
    public ReceiptNotFoundException(String message) {
        super("RECEIPT_NOT_FOUND", message);
    }
}
