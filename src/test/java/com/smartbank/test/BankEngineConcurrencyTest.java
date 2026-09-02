package com.smartbank.test;

import com.smartbank.engine.BankEngine;
import com.smartbank.exceptions.BankingException;
import com.smartbank.exceptions.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency validation suite executing a multi-threaded stress test against
 * the BankEngine's deadlock-free fund transfer mechanism.
 */
public class BankEngineConcurrencyTest {

    private BankEngine engine;
    private final String[] accountNumbers = {
            "ACC-101", "ACC-102", "ACC-103", "ACC-104", "ACC-105",
            "ACC-106", "ACC-107", "ACC-108", "ACC-109", "ACC-110"
    };
    private final double initialBalancePerAccount = 10000.0;

    @BeforeEach
    public void setUp() throws BankingException {
        engine = new BankEngine();
        engine.registerCustomer("CUST-001", "Stress Tester", "stress@test.bank", "+1-555-9999", "1 Test Blvd");

        for (String accNum : accountNumbers) {
            engine.openCheckingAccount(accNum, "CUST-001", initialBalancePerAccount, 5000.0);
        }
    }

    @Test
    @DisplayName("10-Thread High-Throughput Transfer Stress Test verifying Deadlock-Free & Balance Conservation")
    public void testConcurrentTransfersWithBalanceConservation() throws InterruptedException {
        int numberOfThreads = 10;
        int transfersPerThread = 500;
        int totalTransfers = numberOfThreads * transfersPerThread;

        double expectedTotalBalance = initialBalancePerAccount * accountNumbers.length;
        assertEquals(expectedTotalBalance, engine.calculateTotalBankBalance(), 0.001,
                "Initial balance must match expected total.");

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numberOfThreads);
        AtomicInteger successfulTransfers = new AtomicInteger(0);
        AtomicInteger failedDueToFunds = new AtomicInteger(0);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                Random rand = new Random(42 + threadId);
                try {
                    startGate.await(); // Synchronize all 10 threads to start simultaneously
                    for (int j = 0; j < transfersPerThread; j++) {
                        int srcIdx = rand.nextInt(accountNumbers.length);
                        int destIdx = rand.nextInt(accountNumbers.length);
                        while (destIdx == srcIdx) {
                            destIdx = rand.nextInt(accountNumbers.length);
                        }

                        String src = accountNumbers[srcIdx];
                        String dest = accountNumbers[destIdx];
                        double amount = 10.0 + (rand.nextDouble() * 50.0); // $10 - $60 transfer

                        try {
                            engine.transferFunds(src, dest, amount, "Stress Transfer T" + threadId + "-" + j);
                            successfulTransfers.incrementAndGet();
                        } catch (InsufficientBalanceException e) {
                            failedDueToFunds.incrementAndGet();
                        } catch (Exception e) {
                            errors.add(e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.add(e);
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startGate.countDown();

        // Await completion with strict timeout (prevents deadlock hang)
        boolean completedNormally = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();
        boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(completedNormally, "All 10 threads must complete within timeout. Deadlock suspected if timed out!");
        assertTrue(terminated, "Executor service must terminate cleanly.");
        assertTrue(errors.isEmpty(), "No unexpected concurrency or lock errors should occur. Found: " + errors);

        // Verify Strict Conservation of Total Balance
        double actualTotalBalance = engine.calculateTotalBankBalance();
        assertEquals(expectedTotalBalance, actualTotalBalance, 0.001,
                String.format("Total Bank Balance must remain invariant! Expected: %.2f, Actual: %.2f",
                        expectedTotalBalance, actualTotalBalance));

        System.out.printf("[CONCURRENCY TEST PASSED] %d transfers executed (%d succeeded, %d insufficient funds) across 10 threads without deadlock. Conserved Balance: $%,.2f%n",
                totalTransfers, successfulTransfers.get(), failedDueToFunds.get(), actualTotalBalance);
    }
}
