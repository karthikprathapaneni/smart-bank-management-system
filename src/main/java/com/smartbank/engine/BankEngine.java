package com.smartbank.engine;

import com.smartbank.exceptions.AccountNotFoundException;
import com.smartbank.exceptions.BankingException;
import com.smartbank.exceptions.InsufficientBalanceException;
import com.smartbank.exceptions.InvalidTransactionException;
import com.smartbank.model.*;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe core bank engine orchestrating in-memory domain registries,
 * concurrent transaction execution, and deadlock-free fund transfers.
 */
public class BankEngine implements Serializable {
    private static final long serialVersionUID = 1L;

    // Concurrent registries for O(1) concurrent lookups
    private final ConcurrentHashMap<String, Account> accounts;
    private final ConcurrentHashMap<String, Customer> customers;
    private final ConcurrentHashMap<String, Loan> loans;
    private final ConcurrentHashMap<String, Employee> employees;

    // Thread-safe temporal audit trail
    private final CopyOnWriteArrayList<Transaction> transactions;

    public BankEngine() {
        this.accounts = new ConcurrentHashMap<>();
        this.customers = new ConcurrentHashMap<>();
        this.loans = new ConcurrentHashMap<>();
        this.employees = new ConcurrentHashMap<>();
        this.transactions = new CopyOnWriteArrayList<>();
    }

    // =========================================================================
    // Generic Traversal Utility Method
    // =========================================================================
    /**
     * Generic traversal utility method providing a clean iterator over any domain collection.
     *
     * @param <T>        entity type
     * @param collection collection of domain entities
     * @return Iterator of type T
     */
    public <T> Iterator<T> getEntityIterator(Collection<T> collection) {
        if (collection == null) {
            return Collections.emptyIterator();
        }
        return collection.iterator();
    }

    // =========================================================================
    // Customer Operations
    // =========================================================================
    public Customer registerCustomer(String customerId, String fullName, String email, String phone, String address) {
        Customer customer = new Customer(customerId, fullName, email, phone, address);
        customers.put(customer.getCustomerId(), customer);
        return customer;
    }

    public Customer getCustomer(String customerId) {
        return customers.get(customerId);
    }

    public Map<String, Customer> getAllCustomers() {
        return Collections.unmodifiableMap(customers);
    }

    // =========================================================================
    // Account Operations
    // =========================================================================
    public SavingsAccount openSavingsAccount(String accountNumber, String customerId, double initialDeposit, double interestRate)
            throws BankingException {
        Customer customer = customers.get(customerId);
        if (customer == null) {
            throw new BankingException(String.format("Customer [%s] does not exist. Cannot open account.", customerId));
        }
        if (accounts.containsKey(accountNumber)) {
            throw new BankingException(String.format("Account number [%s] already exists.", accountNumber));
        }

        SavingsAccount account = new SavingsAccount(accountNumber, customerId, initialDeposit, interestRate);
        accounts.put(accountNumber, account);
        customer.addAccountNumber(accountNumber);

        if (initialDeposit > 0.0) {
            recordTransaction(new Transaction(accountNumber, Transaction.TransactionType.DEPOSIT,
                    initialDeposit, initialDeposit, null, accountNumber, "Initial account opening deposit"));
        }
        return account;
    }

    public CheckingAccount openCheckingAccount(String accountNumber, String customerId, double initialDeposit, double overdraftLimit)
            throws BankingException {
        Customer customer = customers.get(customerId);
        if (customer == null) {
            throw new BankingException(String.format("Customer [%s] does not exist. Cannot open account.", customerId));
        }
        if (accounts.containsKey(accountNumber)) {
            throw new BankingException(String.format("Account number [%s] already exists.", accountNumber));
        }

        CheckingAccount account = new CheckingAccount(accountNumber, customerId, initialDeposit, overdraftLimit);
        accounts.put(accountNumber, account);
        customer.addAccountNumber(accountNumber);

        if (initialDeposit > 0.0) {
            recordTransaction(new Transaction(accountNumber, Transaction.TransactionType.DEPOSIT,
                    initialDeposit, initialDeposit, null, accountNumber, "Initial account opening deposit"));
        }
        return account;
    }

    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    public Map<String, Account> getAllAccounts() {
        return Collections.unmodifiableMap(accounts);
    }

    // =========================================================================
    // Transactional Core: Deposit & Withdraw
    // =========================================================================
    public void deposit(String accountNumber, double amount, String reference)
            throws BankingException, AccountNotFoundException, InvalidTransactionException {
        Account account = getAccount(accountNumber);
        synchronized (account) {
            account.deposit(amount, reference);
            recordTransaction(new Transaction(accountNumber, Transaction.TransactionType.DEPOSIT,
                    amount, account.getBalance(), null, accountNumber, reference));
        }
    }

    public void deposit(String accountNumber, double amount)
            throws BankingException, AccountNotFoundException, InvalidTransactionException {
        deposit(accountNumber, amount, "Deposit");
    }

    public void withdraw(String accountNumber, double amount, String reference)
            throws BankingException, AccountNotFoundException, InsufficientBalanceException, InvalidTransactionException {
        Account account = getAccount(accountNumber);
        synchronized (account) {
            account.withdraw(amount);
            recordTransaction(new Transaction(accountNumber, Transaction.TransactionType.WITHDRAWAL,
                    amount, account.getBalance(), accountNumber, null, reference));
        }
    }

    public void withdraw(String accountNumber, double amount)
            throws BankingException, AccountNotFoundException, InsufficientBalanceException, InvalidTransactionException {
        withdraw(accountNumber, amount, "Withdrawal");
    }

    // =========================================================================
    // Deadlock-Free Inter-Account Fund Transfer
    // =========================================================================
    /**
     * Executes an atomic transfer between two accounts.
     * Prevents deadlocks by enforcing a Total Natural Lock Ordering based on
     * the lexical ordering of account numbers (String.compareTo).
     *
     * Invariant: Total sum of balances of source and destination accounts before
     * and after the transfer is strictly conserved.
     *
     * @param srcAccNum  source account number
     * @param destAccNum destination account number
     * @param amount     monetary transfer amount
     * @param reference  memo/audit reference
     * @throws BankingException on any validation, insufficient funds, or missing account error
     */
    public void transferFunds(String srcAccNum, String destAccNum, double amount, String reference)
            throws BankingException, AccountNotFoundException, InsufficientBalanceException, InvalidTransactionException {
        if (srcAccNum == null || destAccNum == null) {
            throw new InvalidTransactionException("Source and Destination account numbers must not be null.");
        }
        if (srcAccNum.equals(destAccNum)) {
            throw new InvalidTransactionException("Cannot transfer funds to the same account: " + srcAccNum);
        }
        if (amount <= 0.0) {
            throw new InvalidTransactionException(String.format("Transfer amount must be strictly positive. Provided: %.2f", amount));
        }

        Account srcAccount = getAccount(srcAccNum);
        Account destAccount = getAccount(destAccNum);

        // Determine Total Natural Lock Order to prevent circular wait deadlock
        Account firstLock = (srcAccNum.compareTo(destAccNum) < 0) ? srcAccount : destAccount;
        Account secondLock = (srcAccNum.compareTo(destAccNum) < 0) ? destAccount : srcAccount;

        synchronized (firstLock) {
            synchronized (secondLock) {
                // Balance Conservation Check: Initial Sum
                double initialSum = srcAccount.getBalance() + destAccount.getBalance();

                // Perform withdrawal from source
                srcAccount.withdraw(amount);

                // Perform deposit to destination
                destAccount.deposit(amount, "Transfer from " + srcAccNum);

                // Balance Conservation Check: Post-transfer Sum
                double finalSum = srcAccount.getBalance() + destAccount.getBalance();
                if (Math.abs(initialSum - finalSum) > 0.0001) {
                    throw new BankingException(String.format(
                            "CRITICAL INVARIANT VIOLATION: Conservation of balance failed during transfer from %s to %s. (Initial: %.2f, Final: %.2f)",
                            srcAccNum, destAccNum, initialSum, finalSum));
                }

                // Record audit transactions
                String memo = (reference != null && !reference.isBlank()) ? reference : "Fund Transfer";
                recordTransaction(new Transaction(srcAccNum, Transaction.TransactionType.TRANSFER_OUT,
                        amount, srcAccount.getBalance(), srcAccNum, destAccNum, memo));
                recordTransaction(new Transaction(destAccNum, Transaction.TransactionType.TRANSFER_IN,
                        amount, destAccount.getBalance(), srcAccNum, destAccNum, memo));
            }
        }
    }

    public void transferFunds(String srcAccNum, String destAccNum, double amount)
            throws BankingException, AccountNotFoundException, InsufficientBalanceException, InvalidTransactionException {
        transferFunds(srcAccNum, destAccNum, amount, "Direct Transfer");
    }

    // =========================================================================
    // Loan & Employee Operations
    // =========================================================================
    public Loan createLoan(String loanId, String customerId, double principal, double interestRate, int termMonths)
            throws BankingException {
        if (!customers.containsKey(customerId)) {
            throw new BankingException("Customer not found: " + customerId);
        }
        Loan loan = new Loan(loanId, customerId, principal, interestRate, termMonths);
        loans.put(loanId, loan);
        return loan;
    }

    public Loan getLoan(String loanId) {
        return loans.get(loanId);
    }

    public Map<String, Loan> getAllLoans() {
        return Collections.unmodifiableMap(loans);
    }

    public double payLoan(String loanId, double amount, String paymentAccount)
            throws BankingException, InvalidTransactionException, AccountNotFoundException, InsufficientBalanceException {
        Loan loan = loans.get(loanId);
        if (loan == null) {
            throw new BankingException("Loan not found: " + loanId);
        }
        if (paymentAccount != null) {
            withdraw(paymentAccount, amount, "Loan Repayment for " + loanId);
        }
        double remaining = loan.makePayment(amount);
        recordTransaction(new Transaction(paymentAccount != null ? paymentAccount : "LOAN-SYS",
                Transaction.TransactionType.LOAN_PAYMENT, amount, remaining, paymentAccount, loanId, "Loan payment: " + loanId));
        return remaining;
    }

    public Employee registerEmployee(String employeeId, String fullName, String email, String department, Employee.EmployeeRole role) {
        Employee employee = new Employee(employeeId, fullName, email, department, role);
        employees.put(employeeId, employee);
        return employee;
    }

    public Employee getEmployee(String employeeId) {
        return employees.get(employeeId);
    }

    public Map<String, Employee> getAllEmployees() {
        return Collections.unmodifiableMap(employees);
    }

    // =========================================================================
    // Audit & Invariants
    // =========================================================================
    private void recordTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    /**
     * Calculates the grand total balance across all registered accounts.
     *
     * @return sum of all account balances
     */
    public double calculateTotalBankBalance() {
        double total = 0.0;
        for (Account account : accounts.values()) {
            synchronized (account) {
                total += account.getBalance();
            }
        }
        return total;
    }

    public void clear() {
        accounts.clear();
        customers.clear();
        loans.clear();
        employees.clear();
        transactions.clear();
    }
}
