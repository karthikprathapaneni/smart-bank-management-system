package com.smartbank.interfaces;

import com.smartbank.exceptions.BankingException;
import com.smartbank.exceptions.InsufficientBalanceException;
import com.smartbank.exceptions.InvalidTransactionException;

/**
 * Contract defining standard banking operations on bank accounts.
 */
public interface BankOperations {

    /**
     * Deposits a positive monetary amount into the account.
     *
     * @param amount the amount to deposit (must be strictly > 0)
     * @throws InvalidTransactionException if amount <= 0
     * @throws BankingException on operational failure
     */
    void deposit(double amount) throws BankingException, InvalidTransactionException;

    /**
     * Overloaded deposit method accepting an audit reference/memo.
     *
     * @param amount    the amount to deposit (must be strictly > 0)
     * @param reference an audit note or payment reference
     * @throws InvalidTransactionException if amount <= 0
     * @throws BankingException on operational failure
     */
    void deposit(double amount, String reference) throws BankingException, InvalidTransactionException;

    /**
     * Withdraws funds from the account respecting account type rules and limits.
     *
     * @param amount the amount to withdraw (must be strictly > 0)
     * @throws InsufficientBalanceException if funds/overdraft are insufficient
     * @throws InvalidTransactionException if amount is non-positive or account inactive
     * @throws BankingException on operational failure
     */
    void withdraw(double amount) throws BankingException, InsufficientBalanceException, InvalidTransactionException;

    /**
     * Returns the current balance of the account in a thread-safe manner.
     *
     * @return current balance
     */
    double getBalance();

    /**
     * Returns the unique account number identifying this account.
     *
     * @return account number string
     */
    String getAccountNumber();
}
