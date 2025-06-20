// CardNotFoundException.java
package com.company.receipt.exception;

public class CardNotFoundException extends BaseException {
    public CardNotFoundException(String message) {
        super("CARD_NOT_FOUND", message);
    }
}
