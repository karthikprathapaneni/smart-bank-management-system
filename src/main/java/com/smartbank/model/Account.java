package com.smartbank.model;

import com.smartbank.exceptions.BankingException;
import com.smartbank.exceptions.InsufficientBalanceException;
import com.smartbank.exceptions.InvalidTransactionException;
import com.smartbank.interfaces.BankOperations;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract domain base class representing a financial account.
 * Implements {@link BankOperations} and {@link Serializable} with strict thread safety.
 */
public abstract class Account implements BankOperations, Serializable {
    private static final long serialVersionUID = 1L;

    public enum AccountStatus {
        ACTIVE,
        SUSPENDED,
        CLOSED
    }

    public enum AccountType {
        SAVINGS,
        CHECKING
    }

    protected final String accountNumber;
    protected final String customerId;
    protected final AccountType accountType;
    protected double balance;
    protected AccountStatus status;
    protected final LocalDateTime openedAt;

    protected Account(String accountNumber, String customerId, AccountType accountType, double initialBalance, LocalDateTime openedAt) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.accountType = Objects.requireNonNull(accountType, "Account type cannot be null");
        this.balance = Math.max(0.0, initialBalance);
        this.status = AccountStatus.ACTIVE;
        this.openedAt = (openedAt != null) ? openedAt : LocalDateTime.now();
    }

    protected Account(String accountNumber, String customerId, AccountType accountType, double initialBalance) {
        this(accountNumber, customerId, accountType, initialBalance, LocalDateTime.now());
    }

    @Override
    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    @Override
    public synchronized double getBalance() {
        return balance;
    }

    public synchronized AccountStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(AccountStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    /**
     * Standard deposit method.
     */
    @Override
    public synchronized void deposit(double amount) throws BankingException, InvalidTransactionException {
        deposit(amount, "Standard deposit");
    }

    /**
     * Overloaded deposit method accepting an explicit audit reference.
     */
    @Override
    public synchronized void deposit(double amount, String reference) throws BankingException, InvalidTransactionException {
        if (amount <= 0.0) {
            throw new InvalidTransactionException(String.format("Deposit amount must be strictly positive. Provided: %.2f", amount));
        }
        if (status != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException(String.format("Cannot deposit to non-active account [%s]. Current status: %s", accountNumber, status));
        }
        this.balance += amount;
    }

    /**
     * Abstract polymorphic withdrawal method to be implemented by concrete subclasses.
     */
    @Override
    public abstract void withdraw(double amount) throws BankingException, InsufficientBalanceException, InvalidTransactionException;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountNumber, account.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    @Override
    public String toString() {
        return String.format("%s[AccNum: %s, CustID: %s, Balance: $%,.2f, Status: %s]",
                accountType, accountNumber, customerId, balance, status);
    }
}
