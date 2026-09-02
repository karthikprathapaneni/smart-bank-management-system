package com.smartbank.model;

import com.smartbank.exceptions.BankingException;
import com.smartbank.exceptions.InvalidTransactionException;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a Credit Loan facility granted to a Customer.
 */
public class Loan implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum LoanStatus {
        PENDING,
        APPROVED,
        ACTIVE,
        PAID_OFF,
        DEFAULTED
    }

    private final String loanId;
    private final String customerId;
    private final double principalAmount;
    private final double interestRate;
    private final int termMonths;
    private double balanceRemaining;
    private LoanStatus status;
    private final LocalDateTime disbursedAt;

    public Loan(String loanId,
                String customerId,
                double principalAmount,
                double interestRate,
                int termMonths,
                double balanceRemaining,
                LoanStatus status,
                LocalDateTime disbursedAt) {
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.principalAmount = Math.max(0.0, principalAmount);
        this.interestRate = Math.max(0.0, interestRate);
        this.termMonths = Math.max(1, termMonths);
        this.balanceRemaining = Math.max(0.0, balanceRemaining);
        this.status = (status != null) ? status : LoanStatus.ACTIVE;
        this.disbursedAt = (disbursedAt != null) ? disbursedAt : LocalDateTime.now();
    }

    public Loan(String loanId, String customerId, double principalAmount, double interestRate, int termMonths) {
        this(loanId, customerId, principalAmount, interestRate, termMonths, principalAmount, LoanStatus.ACTIVE, LocalDateTime.now());
    }

    public String getLoanId() {
        return loanId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public double getPrincipalAmount() {
        return principalAmount;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public synchronized double getBalanceRemaining() {
        return balanceRemaining;
    }

    public synchronized LoanStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(LoanStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    /**
     * Applies a payment against the outstanding loan balance in a thread-safe manner.
     *
     * @param amount the payment amount
     * @return remaining balance after payment
     * @throws InvalidTransactionException if amount is non-positive or loan already paid
     * @throws BankingException on operational failure
     */
    public synchronized double makePayment(double amount) throws BankingException, InvalidTransactionException {
        if (amount <= 0.0) {
            throw new InvalidTransactionException(String.format("Payment amount must be positive. Provided: %.2f", amount));
        }
        if (status == LoanStatus.PAID_OFF) {
            throw new InvalidTransactionException(String.format("Loan [%s] is already fully paid off.", loanId));
        }
        this.balanceRemaining -= amount;
        if (this.balanceRemaining <= 0.0) {
            this.balanceRemaining = 0.0;
            this.status = LoanStatus.PAID_OFF;
        }
        return this.balanceRemaining;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Loan loan = (Loan) o;
        return Objects.equals(loanId, loan.loanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId);
    }

    @Override
    public String toString() {
        return String.format("Loan[ID: %s, CustID: %s, Principal: $%,.2f, Remaining: $%,.2f, Rate: %.2f%%, Status: %s]",
                loanId, customerId, principalAmount, balanceRemaining, interestRate * 100, status);
    }
}
