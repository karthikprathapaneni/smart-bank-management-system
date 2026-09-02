package com.smartbank.model;

import com.smartbank.exceptions.BankingException;
import com.smartbank.exceptions.InsufficientBalanceException;
import com.smartbank.exceptions.InvalidTransactionException;

import java.time.LocalDateTime;

/**
 * Concrete Checking Account implementation supporting credit overdraft facilities.
 */
public class CheckingAccount extends Account {
    private static final long serialVersionUID = 1L;

    private double overdraftLimit;

    public CheckingAccount(String accountNumber,
                           String customerId,
                           double initialBalance,
                           double overdraftLimit,
                           LocalDateTime openedAt) {
        super(accountNumber, customerId, AccountType.CHECKING, initialBalance, openedAt);
        this.overdraftLimit = Math.max(0.0, overdraftLimit);
    }

    public CheckingAccount(String accountNumber,
                           String customerId,
                           double initialBalance,
                           double overdraftLimit) {
        this(accountNumber, customerId, initialBalance, overdraftLimit, LocalDateTime.now());
    }

    public synchronized double getOverdraftLimit() {
        return overdraftLimit;
    }

    public synchronized void setOverdraftLimit(double overdraftLimit) {
        if (overdraftLimit < 0.0) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative.");
        }
        this.overdraftLimit = overdraftLimit;
    }

    /**
     * Calculates the total purchasing power including balance and overdraft reserve.
     *
     * @return available total funds
     */
    public synchronized double getAvailableFunds() {
        return this.balance + this.overdraftLimit;
    }

    /**
     * Specialized polymorphic withdrawal for Checking Account.
     * Allows balance to become negative up to -overdraftLimit.
     */
    @Override
    public synchronized void withdraw(double amount) throws BankingException, InsufficientBalanceException, InvalidTransactionException {
        if (amount <= 0.0) {
            throw new InvalidTransactionException(String.format("Withdrawal amount must be strictly positive. Provided: %.2f", amount));
        }
        if (status != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException(String.format("Account [%s] is not ACTIVE (Status: %s)", accountNumber, status));
        }
        if (this.balance - amount < -this.overdraftLimit) {
            throw new InsufficientBalanceException(
                    String.format("Withdrawal of $%,.2f exceeds available funds $%,.2f (Balance: $%,.2f, Overdraft: $%,.2f) on [%s]",
                            amount, getAvailableFunds(), this.balance, this.overdraftLimit, accountNumber));
        }
        this.balance -= amount;
    }

    @Override
    public String toString() {
        return String.format("CheckingAccount[AccNum: %s, CustID: %s, Balance: $%,.2f, OverdraftLimit: $%,.2f, Status: %s]",
                accountNumber, customerId, balance, overdraftLimit, status);
    }
}
