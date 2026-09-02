package com.smartbank.model;

import com.smartbank.exceptions.BankingException;
import com.smartbank.exceptions.InsufficientBalanceException;
import com.smartbank.exceptions.InvalidTransactionException;

import java.time.LocalDateTime;

/**
 * Concrete Savings Account implementation offering interest accrual
 * and strict non-negative balance enforcement.
 */
public class SavingsAccount extends Account {
    private static final long serialVersionUID = 1L;

    private double interestRate; // Annualized rate, e.g., 0.045 for 4.5%
    private double minimumBalance;

    public SavingsAccount(String accountNumber,
                          String customerId,
                          double initialBalance,
                          double interestRate,
                          double minimumBalance,
                          LocalDateTime openedAt) {
        super(accountNumber, customerId, AccountType.SAVINGS, initialBalance, openedAt);
        this.interestRate = Math.max(0.0, interestRate);
        this.minimumBalance = Math.max(0.0, minimumBalance);
    }

    public SavingsAccount(String accountNumber,
                          String customerId,
                          double initialBalance,
                          double interestRate) {
        this(accountNumber, customerId, initialBalance, interestRate, 0.0, LocalDateTime.now());
    }

    public synchronized double getInterestRate() {
        return interestRate;
    }

    public synchronized void setInterestRate(double interestRate) {
        if (interestRate < 0.0) {
            throw new IllegalArgumentException("Interest rate cannot be negative.");
        }
        this.interestRate = interestRate;
    }

    public synchronized double getMinimumBalance() {
        return minimumBalance;
    }

    public synchronized void setMinimumBalance(double minimumBalance) {
        this.minimumBalance = Math.max(0.0, minimumBalance);
    }

    /**
     * Calculates the periodic interest accrued based on current balance.
     *
     * @return calculated interest amount
     */
    public synchronized double calculateInterest() {
        return this.balance * this.interestRate;
    }

    /**
     * Accrues and deposits the calculated interest to the balance.
     *
     * @return the amount of interest applied
     * @throws BankingException on operational failure
     */
    public synchronized double applyInterest() throws BankingException {
        double interest = calculateInterest();
        if (interest > 0.0) {
            deposit(interest, "Accrued interest deposit");
        }
        return interest;
    }

    /**
     * Specialized polymorphic withdrawal for Savings Account.
     * Disallows negative balances and enforces minimum balance threshold.
     */
    @Override
    public synchronized void withdraw(double amount) throws BankingException, InsufficientBalanceException, InvalidTransactionException {
        if (amount <= 0.0) {
            throw new InvalidTransactionException(String.format("Withdrawal amount must be strictly positive. Provided: %.2f", amount));
        }
        if (status != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException(String.format("Account [%s] is not ACTIVE (Status: %s)", accountNumber, status));
        }
        if (this.balance - amount < this.minimumBalance) {
            throw new InsufficientBalanceException(accountNumber, amount, this.balance);
        }
        this.balance -= amount;
    }

    @Override
    public String toString() {
        return String.format("SavingsAccount[AccNum: %s, CustID: %s, Balance: $%,.2f, Rate: %.2f%%, Status: %s]",
                accountNumber, customerId, balance, interestRate * 100, status);
    }
}
