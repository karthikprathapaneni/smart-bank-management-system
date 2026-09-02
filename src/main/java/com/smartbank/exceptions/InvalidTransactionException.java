package com.smartbank.exceptions;

/**
 * Checked exception thrown when an invalid transaction parameter or invariant
 * violation is detected (e.g., negative amounts, circular transfers, or suspended accounts).
 */
public class InvalidTransactionException extends BankingException {
    private static final long serialVersionUID = 1L;

    public InvalidTransactionException(String message) {
        super(message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
