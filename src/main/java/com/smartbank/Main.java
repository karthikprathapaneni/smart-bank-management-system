package com.smartbank;

import com.smartbank.engine.BankEngine;
import com.smartbank.exceptions.BankingException;
import com.smartbank.gui.SmartBankGUI;
import com.smartbank.model.Employee;
import com.smartbank.persistence.PersistenceManager;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.io.File;

/**
 * Main application bootstrap for the Smart Bank Management System (SBMS).
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  Smart Bank Management System (SBMS) - CSA09 Production Suite");
        System.out.println("===============================================================");

        BankEngine engine = new BankEngine();
        PersistenceManager persistence = new PersistenceManager();

        // Check if serialized state exists, otherwise initialize baseline demo data
        File snapshot = new File("bank_data.ser");
        if (snapshot.exists() && snapshot.length() > 0) {
            try {
                System.out.println("[BOOTSTRAP] Found existing snapshot. Restoring engine state...");
                BankEngine loaded = persistence.deserializeState("bank_data.ser");
                loaded.getAllCustomers().values().forEach(c ->
                        engine.registerCustomer(c.getCustomerId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getAddress()));
                loaded.getAllAccounts().values().forEach(a -> {
                    try {
                        if (a instanceof com.smartbank.model.SavingsAccount sa) {
                            engine.openSavingsAccount(sa.getAccountNumber(), sa.getCustomerId(), sa.getBalance(), sa.getInterestRate());
                        } else if (a instanceof com.smartbank.model.CheckingAccount ca) {
                            engine.openCheckingAccount(ca.getAccountNumber(), ca.getCustomerId(), ca.getBalance(), ca.getOverdraftLimit());
                        }
                    } catch (BankingException e) {
                        System.err.println("[BOOTSTRAP-WARN] Error restoring account: " + e.getMessage());
                    }
                });
                System.out.println("[BOOTSTRAP] Successfully restored state from bank_data.ser.");
            } catch (Exception e) {
                System.err.println("[BOOTSTRAP-ERROR] Failed to restore snapshot: " + e.getMessage() + ". Initializing fresh baseline.");
                seedBaselineData(engine);
            }
        } else {
            seedBaselineData(engine);
        }

        // Check if graphics environment is available
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[BOOTSTRAP] Headless environment detected. AWT GUI omitted.");
            System.out.printf("[BOOTSTRAP] Registered Customers: %d | Accounts: %d | Total Assets: $%,.2f%n",
                    engine.getAllCustomers().size(), engine.getAllAccounts().size(), engine.calculateTotalBankBalance());
            return;
        }

        // Launch Native AWT GUI safely on the Event Dispatch Thread (EDT)
        EventQueue.invokeLater(() -> {
            try {
                SmartBankGUI gui = new SmartBankGUI(engine);
                gui.setVisible(true);
                System.out.println("[BOOTSTRAP] Smart Bank AWT GUI started successfully on EDT.");
            } catch (Exception e) {
                System.err.println("[BOOTSTRAP-ERROR] Error launching GUI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Seeds in-memory baseline demo customers, accounts, loans, and employees.
     */
    public static void seedBaselineData(BankEngine engine) {
        System.out.println("[BOOTSTRAP] Seeding baseline banking demo data...");
        try {
            // 1. Customers
            engine.registerCustomer("CUST-1001", "Alice Johnson", "alice.johnson@example.com", "+1-555-0101", "100 Financial Way, New York, NY");
            engine.registerCustomer("CUST-1002", "Bob Smith", "bob.smith@example.com", "+1-555-0102", "250 Enterprise Blvd, Chicago, IL");
            engine.registerCustomer("CUST-1003", "Charlie Davis", "charlie.davis@example.com", "+1-555-0103", "742 Evergreen Terrace, Springfield, OR");
            engine.registerCustomer("CUST-1004", "Diana Prince", "diana.prince@example.com", "+1-555-0104", "1407 Graymalkin Lane, Salem, MA");

            // 2. Accounts
            engine.openSavingsAccount("ACC-SAV-1001", "CUST-1001", 25000.00, 0.045);
            engine.openCheckingAccount("ACC-CHK-1002", "CUST-1001", 5000.00, 1500.00);
            engine.openSavingsAccount("ACC-SAV-2001", "CUST-1002", 12500.00, 0.040);
            engine.openCheckingAccount("ACC-CHK-2002", "CUST-1002", 3200.00, 1000.00);
            engine.openSavingsAccount("ACC-SAV-3001", "CUST-1003", 8900.00, 0.038);
            engine.openCheckingAccount("ACC-CHK-4001", "CUST-1004", 15000.00, 2500.00);

            // 3. Loans
            engine.createLoan("LOAN-9001", "CUST-1001", 50000.00, 0.065, 60);
            engine.createLoan("LOAN-9002", "CUST-1002", 15000.00, 0.072, 36);

            // 4. Employees
            engine.registerEmployee("EMP-001", "Marcus Vance", "marcus.vance@smartbank.internal", "Operations", Employee.EmployeeRole.MANAGER);
            engine.registerEmployee("EMP-002", "Sarah Jenkins", "sarah.jenkins@smartbank.internal", "Retail Banking", Employee.EmployeeRole.TELLER);

            System.out.printf("[BOOTSTRAP] Demo baseline loaded: %d accounts, total balance $%,.2f%n",
                    engine.getAllAccounts().size(), engine.calculateTotalBankBalance());
        } catch (BankingException e) {
            System.err.println("[BOOTSTRAP-ERROR] Error seeding baseline data: " + e.getMessage());
        }
    }
}
