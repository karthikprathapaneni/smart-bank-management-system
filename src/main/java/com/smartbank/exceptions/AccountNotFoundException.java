package com.smartbank.exceptions;

/**
 * Checked exception thrown when an account lookup fails in the bank registry.
 */
public class AccountNotFoundException extends BankingException {
    private static final long serialVersionUID = 1L;

    private final String accountNumber;

    public AccountNotFoundException(String accountNumber) {
        super(String.format("Account [%s] was not found in the bank registry.", accountNumber));
        this.accountNumber = accountNumber;
    }

    public AccountNotFoundException(String accountNumber, String customMessage) {
        super(customMessage);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
