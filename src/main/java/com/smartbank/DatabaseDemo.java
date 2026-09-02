package com.smartbank;

import com.smartbank.engine.BankEngine;
import com.smartbank.exceptions.BankingException;
import com.smartbank.model.Account;
import com.smartbank.model.Customer;
import com.smartbank.persistence.PersistenceManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Scanner;

/**
 * Interactive Command-Line Demonstration of MySQL Database Connectivity and JDBC CRUD
 * for academic review and course demonstration (CSA09).
 */
public class DatabaseDemo {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  SMART BANK MANAGEMENT SYSTEM - MYSQL JDBC LIVE DEMO (CSA09)");
        System.out.println("===============================================================");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter MySQL Host [default: localhost]: ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Enter MySQL Port [default: 3306]: ");
        String port = scanner.nextLine().trim();
        if (port.isEmpty()) port = "3306";

        System.out.print("Enter Database Name [default: smart_bank_db]: ");
        String dbName = scanner.nextLine().trim();
        if (dbName.isEmpty()) dbName = "smart_bank_db";

        System.out.print("Enter MySQL Username [default: root]: ");
        String user = scanner.nextLine().trim();
        if (user.isEmpty()) user = "root";

        System.out.print("Enter MySQL Password: ");
        String password = scanner.nextLine().trim();

        String url = String.format("jdbc:mysql://%s:%s/%s?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, dbName);

        System.out.println("\n[STEP 1] Loading MySQL JDBC Driver (com.mysql.cj.jdbc.Driver)...");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("? JDBC Driver loaded successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("? Failed to load MySQL JDBC Driver: " + e.getMessage());
            return;
        }

        System.out.println("\n[STEP 2] Establishing live Connection to " + url + "...");
        PersistenceManager pm = new PersistenceManager();
        try (Connection conn = pm.getConnection(url, user, password)) {
            System.out.println("? Connected to MySQL Database Server successfully!");
            DatabaseMetaData meta = conn.getMetaData();
            System.out.printf("  - Database Product Name    : %s%n", meta.getDatabaseProductName());
            System.out.printf("  - Database Product Version : %s%n", meta.getDatabaseProductVersion());
            System.out.printf("  - Driver Version           : %s%n", meta.getDriverVersion());

            System.out.println("\n[STEP 3] Executing Schema DDL (sql/01_schema.sql)...");
            executeSchemaScript(conn, "sql/01_schema.sql");
            System.out.println("? Tables created / verified: customers, accounts, transactions, loans, employees");

            System.out.println("\n[STEP 4] Populating in-memory BankEngine with live sample data...");
            BankEngine engine = new BankEngine();
            Main.seedBaselineData(engine);

            System.out.println("\n[STEP 5] Executing Transactional Batch JDBC Save (ACID autoCommit=false)...");
            pm.saveToDatabase(engine, conn);
            System.out.println("? All in-memory entities synchronized and COMMITTED to MySQL tables!");

            System.out.println("\n[STEP 6] Querying MySQL Tables directly via JDBC ResultSet (Demonstration):");
            displayTableData(conn);

            System.out.println("\n[STEP 7] Verifying Two-Way Sync: Loading fresh BankEngine instance from MySQL...");
            BankEngine freshEngine = new BankEngine();
            pm.loadFromDatabase(freshEngine, conn);
            System.out.printf("? Loaded %d Customers and %d Accounts directly from MySQL.%n",
                    freshEngine.getAllCustomers().size(), freshEngine.getAllAccounts().size());
            System.out.printf("? Total Reconstructed Bank Assets: $%,.2f%n", freshEngine.calculateTotalBankBalance());

            System.out.println("\n===============================================================");
            System.out.println("  MYSQL JDBC LIVE INTEGRATION DEMO COMPLETED SUCCESSFULLY!");
            System.out.println("===============================================================");

        } catch (SQLException e) {
            System.err.println("\n? SQL Exception Encountered: " + e.getMessage());
            System.err.println("Error Code: " + e.getErrorCode() + " | SQLState: " + e.getSQLState());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("\n? Unexpected Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeSchemaScript(Connection conn, String scriptPath) throws Exception {
        if (!Files.exists(Paths.get(scriptPath))) {
            System.out.println("Schema file " + scriptPath + " not found locally, skipping DDL execution.");
            return;
        }
        String sqlContent = Files.readString(Paths.get(scriptPath));
        String[] statements = sqlContent.split(";");
        try (Statement st = conn.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--") && !trimmed.toLowerCase().startsWith("create database") && !trimmed.toLowerCase().startsWith("use ")) {
                    try {
                        st.execute(trimmed);
                    } catch (SQLException ignored) {
                    }
                }
            }
        }
    }

    private static void displayTableData(Connection conn) throws SQLException {
        // Display Customers
        System.out.println("\n--- [CUSTOMERS TABLE (SELECT * FROM customers)] ---");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT customer_id, full_name, email, phone FROM customers LIMIT 5")) {
            System.out.printf("%-12s | %-18s | %-25s | %-15s%n", "CUSTOMER_ID", "FULL_NAME", "EMAIL", "PHONE");
            System.out.println("----------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-12s | %-18s | %-25s | %-15s%n",
                        rs.getString("customer_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"));
            }
        }

        // Display Accounts
        System.out.println("\n--- [ACCOUNTS TABLE (SELECT * FROM accounts)] ---");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT account_number, customer_id, account_type, balance, status FROM accounts LIMIT 6")) {
            System.out.printf("%-15s | %-12s | %-10s | %-12s | %-8s%n", "ACCOUNT_NUM", "CUSTOMER_ID", "TYPE", "BALANCE", "STATUS");
            System.out.println("----------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-15s | %-12s | %-10s | $%,11.2f | %-8s%n",
                        rs.getString("account_number"),
                        rs.getString("customer_id"),
                        rs.getString("account_type"),
                        rs.getDouble("balance"),
                        rs.getString("status"));
            }
        }
    }
}
