package com.smartbank.exceptions;

/**
 * Checked exception thrown when a withdrawal or fund transfer violates
 * available balance or permitted overdraft limits.
 */
public class InsufficientBalanceException extends BankingException {
    private static final long serialVersionUID = 1L;

    private final String accountNumber;
    private final double requestedAmount;
    private final double currentBalance;

    public InsufficientBalanceException(String accountNumber, double requestedAmount, double currentBalance) {
        super(String.format("Insufficient funds in account [%s]: requested %.2f, current balance %.2f",
                accountNumber, requestedAmount, currentBalance));
        this.accountNumber = accountNumber;
        this.requestedAmount = requestedAmount;
        this.currentBalance = currentBalance;
    }

    public InsufficientBalanceException(String message) {
        super(message);
        this.accountNumber = null;
        this.requestedAmount = 0.0;
        this.currentBalance = 0.0;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getRequestedAmount() {
        return requestedAmount;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }
}
