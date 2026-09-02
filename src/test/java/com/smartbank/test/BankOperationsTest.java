package com.smartbank.test;

import com.smartbank.engine.BankEngine;
import com.smartbank.exceptions.AccountNotFoundException;
import com.smartbank.exceptions.BankingException;
import com.smartbank.exceptions.InsufficientBalanceException;
import com.smartbank.exceptions.InvalidTransactionException;
import com.smartbank.model.*;
import com.smartbank.persistence.PersistenceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Unit Test Suite validating OOP hierarchy, method overloading,
 * method overriding, checked exceptions, and multi-tier persistence.
 */
public class BankOperationsTest {

    private BankEngine engine;
    private PersistenceManager persistenceManager;
    private static final String TEST_SER_FILE = "test_bank_data.ser";
    private static final String TEST_LOG_FILE = "test_audit_log.txt";

    @BeforeEach
    public void setUp() throws BankingException {
        engine = new BankEngine();
        persistenceManager = new PersistenceManager();

        engine.registerCustomer("CUST-100", "Alice Cooper", "alice@music.org", "+1-555-1234", "10 Rock St");
        engine.openSavingsAccount("SAV-101", "CUST-100", 1000.0, 0.05); // 5% interest
        engine.openCheckingAccount("CHK-102", "CUST-100", 500.0, 300.0);  // $300 overdraft
    }

    @AfterEach
    public void tearDown() {
        new File(TEST_SER_FILE).delete();
        new File(TEST_LOG_FILE).delete();
    }

    @Test
    @DisplayName("Test Method Overloading: Deposit with and without audit reference")
    public void testDepositMethodOverloading() throws BankingException, AccountNotFoundException, InvalidTransactionException {
        Account sav = engine.getAccount("SAV-101");

        // Overloaded deposit(amount)
        engine.deposit("SAV-101", 200.0);
        assertEquals(1200.0, sav.getBalance(), 0.001);

        // Overloaded deposit(amount, reference)
        engine.deposit("SAV-101", 300.0, "Salary Bonus Ref #994");
        assertEquals(1500.0, sav.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Test Method Overriding: SavingsAccount enforces non-negative balance")
    public void testSavingsAccountWithdrawalRule() throws AccountNotFoundException {
        Account sav = engine.getAccount("SAV-101");
        assertTrue(sav instanceof SavingsAccount);

        // Valid withdrawal
        assertDoesNotThrow(() -> engine.withdraw("SAV-101", 400.0));
        assertEquals(600.0, sav.getBalance(), 0.001);

        // Invalid withdrawal exceeding balance
        assertThrows(InsufficientBalanceException.class, () -> engine.withdraw("SAV-101", 700.0));
    }

    @Test
    @DisplayName("Test Method Overriding: CheckingAccount honors overdraft facility")
    public void testCheckingAccountOverdraftWithdrawalRule() throws Exception {
        Account chk = engine.getAccount("CHK-102");
        assertTrue(chk instanceof CheckingAccount);

        // Withdraw below 0 within overdraft limit ($500 balance + $300 overdraft = $800 max)
        engine.withdraw("CHK-102", 700.0, "Emergency Rent");
        assertEquals(-200.0, chk.getBalance(), 0.001);

        // Exceed overdraft limit
        assertThrows(InsufficientBalanceException.class, () -> engine.withdraw("CHK-102", 150.0));
    }

    @Test
    @DisplayName("Test Savings Interest Calculation and Accrual")
    public void testSavingsInterestAccrual() throws AccountNotFoundException, BankingException {
        SavingsAccount sav = (SavingsAccount) engine.getAccount("SAV-101");
        double interest = sav.applyInterest();

        assertEquals(50.0, interest, 0.001); // 1000 * 0.05
        assertEquals(1050.0, sav.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Test Checked Exception: Invalid Transaction with negative or zero amounts")
    public void testInvalidTransactionExceptions() {
        assertThrows(InvalidTransactionException.class, () -> engine.deposit("SAV-101", -50.0));
        assertThrows(InvalidTransactionException.class, () -> engine.deposit("SAV-101", 0.0));
        assertThrows(InvalidTransactionException.class, () -> engine.withdraw("SAV-101", -10.0));
        assertThrows(InvalidTransactionException.class, () -> engine.transferFunds("SAV-101", "CHK-102", -100.0));
        assertThrows(InvalidTransactionException.class, () -> engine.transferFunds("SAV-101", "SAV-101", 50.0));
    }

    @Test
    @DisplayName("Test Checked Exception: AccountNotFoundException on unknown account lookup")
    public void testAccountNotFoundException() {
        assertThrows(AccountNotFoundException.class, () -> engine.getAccount("UNKNOWN-999"));
        assertThrows(AccountNotFoundException.class, () -> engine.deposit("UNKNOWN-999", 100.0));
    }

    @Test
    @DisplayName("Test Generic Traversal Utility Method")
    public void testGenericTraversalUtility() {
        Iterator<Account> it = engine.getEntityIterator(engine.getAllAccounts().values());
        assertNotNull(it);
        int count = 0;
        while (it.hasNext()) {
            Account acc = it.next();
            assertNotNull(acc);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Test Java Binary Serialization and Deserialization Roundtrip")
    public void testBinarySerializationRoundtrip() throws IOException, ClassNotFoundException, AccountNotFoundException {
        // Serialize
        persistenceManager.serializeState(engine, TEST_SER_FILE);
        assertTrue(new File(TEST_SER_FILE).exists());

        // Deserialize
        BankEngine deserialized = persistenceManager.deserializeState(TEST_SER_FILE);
        assertNotNull(deserialized);
        assertEquals(1, deserialized.getAllCustomers().size());
        assertEquals(2, deserialized.getAllAccounts().size());

        Account sav = deserialized.getAccount("SAV-101");
        assertEquals(1000.0, sav.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Test Flat File Append-Only Audit Logging")
    public void testFlatFileAuditLogging() throws IOException {
        Transaction txn1 = new Transaction("SAV-101", Transaction.TransactionType.DEPOSIT, 500.0, 1500.0, null, "SAV-101", "Audit Test 1");
        Transaction txn2 = new Transaction("CHK-102", Transaction.TransactionType.WITHDRAWAL, 200.0, 300.0, "CHK-102", null, "Audit Test 2");

        persistenceManager.appendAuditLog(txn1, TEST_LOG_FILE);
        persistenceManager.appendAuditLog(txn2, TEST_LOG_FILE);

        List<String> logLines = persistenceManager.readAuditLog(TEST_LOG_FILE);
        assertEquals(2, logLines.size());
        assertTrue(logLines.get(0).contains("SAV-101"));
        assertTrue(logLines.get(1).contains("CHK-102"));
    }
}
