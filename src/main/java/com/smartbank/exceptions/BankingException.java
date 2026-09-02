package com.smartbank.exceptions;

/**
 * Base checked exception for all domain and operational failures within
 * the Smart Bank Management System.
 */
public class BankingException extends Exception {
    private static final long serialVersionUID = 1L;

    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
