package com.smartbank.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable transaction record representing an atomic financial movement.
 */
public class Transaction implements Serializable, Comparable<Transaction> {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER_IN,
        TRANSFER_OUT,
        INTEREST,
        LOAN_PAYMENT
    }

    private final String transactionId;
    private final String accountNumber;
    private final TransactionType type;
    private final double amount;
    private final double balanceAfter;
    private final String sourceAccount;
    private final String destinationAccount;
    private final String referenceNote;
    private final LocalDateTime timestamp;

    public Transaction(String transactionId,
                       String accountNumber,
                       TransactionType type,
                       double amount,
                       double balanceAfter,
                       String sourceAccount,
                       String destinationAccount,
                       String referenceNote,
                       LocalDateTime timestamp) {
        this.transactionId = (transactionId != null && !transactionId.isBlank()) 
                ? transactionId 
                : "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.referenceNote = (referenceNote != null) ? referenceNote : "";
        this.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
    }

    public Transaction(String accountNumber,
                       TransactionType type,
                       double amount,
                       double balanceAfter,
                       String sourceAccount,
                       String destinationAccount,
                       String referenceNote) {
        this(null, accountNumber, type, amount, balanceAfter, sourceAccount, destinationAccount, referenceNote, LocalDateTime.now());
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public String getDestinationAccount() {
        return destinationAccount;
    }

    public String getReferenceNote() {
        return referenceNote;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    @Override
    public int compareTo(Transaction other) {
        return this.timestamp.compareTo(other.timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return String.format("[%s] ID:%s | Acc:%s | Type:%-12s | Amt:$%,10.2f | BalAfter:$%,10.2f | Ref:%s",
                getFormattedTimestamp(), transactionId, accountNumber, type, amount, balanceAfter, referenceNote);
    }
}
