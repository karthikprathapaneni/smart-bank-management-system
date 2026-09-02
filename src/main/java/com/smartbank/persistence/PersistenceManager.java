package com.smartbank.persistence;

import com.smartbank.engine.BankEngine;
import com.smartbank.exceptions.BankingException;
import com.smartbank.model.*;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise Persistence Manager providing multi-tier persistence strategies:
 * 1. Java Binary Object Serialization (bank_data.ser)
 * 2. Flat-file Append-Only Audit Logging (audit_log.txt)
 * 3. JDBC Relational Transactional CRUD against MySQL with rollback safety
 */
public class PersistenceManager {

    private static final String DEFAULT_SERIALIZATION_PATH = "bank_data.ser";
    private static final String DEFAULT_AUDIT_LOG_PATH = "audit_log.txt";

    // =========================================================================
    // 1. Java Binary Object Serialization
    // =========================================================================
    /**
     * Serializes the in-memory state of BankEngine to a binary snapshot file.
     *
     * @param engine   the BankEngine instance to serialize
     * @param filePath target file destination
     * @throws IOException on write error
     */
    public synchronized void serializeState(BankEngine engine, String filePath) throws IOException {
        String targetPath = (filePath != null && !filePath.isBlank()) ? filePath : DEFAULT_SERIALIZATION_PATH;
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(targetPath)))) {
            oos.writeObject(engine);
            oos.flush();
        }
    }

    /**
     * Deserializes the BankEngine state from a binary snapshot file.
     *
     * @param filePath source file destination
     * @return restored BankEngine instance
     * @throws IOException            on read error
     * @throws ClassNotFoundException if serialized classes are not found
     */
    public synchronized BankEngine deserializeState(String filePath) throws IOException, ClassNotFoundException {
        String targetPath = (filePath != null && !filePath.isBlank()) ? filePath : DEFAULT_SERIALIZATION_PATH;
        File file = new File(targetPath);
        if (!file.exists() || file.length() == 0) {
            throw new FileNotFoundException("Serialized state file not found: " + targetPath);
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            return (BankEngine) ois.readObject();
        }
    }

    // =========================================================================
    // 2. Flat File Append-Only Audit Log
    // =========================================================================
    /**
     * Appends a transaction record to the flat-file audit log.
     *
     * @param transaction transaction instance
     * @param filePath    target log file path
     * @throws IOException on write error
     */
    public synchronized void appendAuditLog(Transaction transaction, String filePath) throws IOException {
        String targetPath = (filePath != null && !filePath.isBlank()) ? filePath : DEFAULT_AUDIT_LOG_PATH;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetPath, true))) {
            writer.write(transaction.toString());
            writer.newLine();
            writer.flush();
        }
    }

    /**
     * Reads all lines from the flat-file audit log.
     *
     * @param filePath source log file path
     * @return list of log lines
     * @throws IOException on read error
     */
    public synchronized List<String> readAuditLog(String filePath) throws IOException {
        String targetPath = (filePath != null && !filePath.isBlank()) ? filePath : DEFAULT_AUDIT_LOG_PATH;
        File file = new File(targetPath);
        List<String> lines = new ArrayList<>();
        if (!file.exists()) {
            return lines;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    // =========================================================================
    // 3. JDBC Relational Transactional Persistence
    // =========================================================================
    /**
     * Creates a standard JDBC Connection to the configured MySQL database.
     *
     * @param url      JDBC connection URL
     * @param user     database username
     * @param password database password
     * @return active JDBC Connection
     * @throws SQLException on connection failure
     */
    public Connection getConnection(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Synchronizes in-memory BankEngine entities to the relational database inside
     * a single atomic transaction. Automatically rolls back on any SQL failure.
     *
     * @param engine BankEngine state
     * @param conn   active JDBC Connection
     * @throws SQLException on database error
     */
    public synchronized void saveToDatabase(BankEngine engine, Connection conn) throws SQLException {
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Cannot save state: JDBC connection is null or closed.");
        }

        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false); // Begin ACID Transaction

            // 1. Upsert Customers
            String sqlCust = "INSERT INTO customers (customer_id, full_name, email, phone, address, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), email=VALUES(email), phone=VALUES(phone), address=VALUES(address)";
            try (PreparedStatement ps = conn.prepareStatement(sqlCust)) {
                for (Customer cust : engine.getAllCustomers().values()) {
                    ps.setString(1, cust.getCustomerId());
                    ps.setString(2, cust.getFullName());
                    ps.setString(3, cust.getEmail());
                    ps.setString(4, cust.getPhone());
                    ps.setString(5, cust.getAddress());
                    ps.setTimestamp(6, Timestamp.valueOf(cust.getCreatedAt()));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 2. Upsert Accounts
            String sqlAcc = "INSERT INTO accounts (account_number, customer_id, account_type, balance, interest_rate, overdraft_limit, status, opened_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE balance=VALUES(balance), interest_rate=VALUES(interest_rate), overdraft_limit=VALUES(overdraft_limit), status=VALUES(status)";
            try (PreparedStatement ps = conn.prepareStatement(sqlAcc)) {
                for (Account acc : engine.getAllAccounts().values()) {
                    ps.setString(1, acc.getAccountNumber());
                    ps.setString(2, acc.getCustomerId());
                    ps.setString(3, acc.getAccountType().name());
                    ps.setDouble(4, acc.getBalance());
                    if (acc instanceof SavingsAccount sa) {
                        ps.setDouble(5, sa.getInterestRate());
                        ps.setDouble(6, 0.0);
                    } else if (acc instanceof CheckingAccount ca) {
                        ps.setDouble(5, 0.0);
                        ps.setDouble(6, ca.getOverdraftLimit());
                    } else {
                        ps.setDouble(5, 0.0);
                        ps.setDouble(6, 0.0);
                    }
                    ps.setString(7, acc.getStatus().name());
                    ps.setTimestamp(8, Timestamp.valueOf(acc.getOpenedAt()));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 3. Insert Transactions (Ignore duplicates if already present)
            String sqlTxn = "INSERT IGNORE INTO transactions (transaction_id, account_number, transaction_type, amount, balance_after, source_account, destination_account, reference_note, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlTxn)) {
                for (Transaction txn : engine.getTransactions()) {
                    ps.setString(1, txn.getTransactionId());
                    ps.setString(2, txn.getAccountNumber());
                    ps.setString(3, txn.getType().name());
                    ps.setDouble(4, txn.getAmount());
                    ps.setDouble(5, txn.getBalanceAfter());
                    ps.setString(6, txn.getSourceAccount());
                    ps.setString(7, txn.getDestinationAccount());
                    ps.setString(8, txn.getReferenceNote());
                    ps.setTimestamp(9, Timestamp.valueOf(txn.getTimestamp()));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 4. Upsert Loans
            String sqlLoan = "INSERT INTO loans (loan_id, customer_id, principal_amount, interest_rate, term_months, balance_remaining, status, disbursed_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE balance_remaining=VALUES(balance_remaining), status=VALUES(status)";
            try (PreparedStatement ps = conn.prepareStatement(sqlLoan)) {
                for (Loan loan : engine.getAllLoans().values()) {
                    ps.setString(1, loan.getLoanId());
                    ps.setString(2, loan.getCustomerId());
                    ps.setDouble(3, loan.getPrincipalAmount());
                    ps.setDouble(4, loan.getInterestRate());
                    ps.setInt(5, loan.getTermMonths());
                    ps.setDouble(6, loan.getBalanceRemaining());
                    ps.setString(7, loan.getStatus().name());
                    ps.setTimestamp(8, Timestamp.valueOf(loan.getDisbursedAt()));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 5. Upsert Employees
            String sqlEmp = "INSERT INTO employees (employee_id, full_name, email, department, role, hire_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), email=VALUES(email), department=VALUES(department), role=VALUES(role)";
            try (PreparedStatement ps = conn.prepareStatement(sqlEmp)) {
                for (Employee emp : engine.getAllEmployees().values()) {
                    ps.setString(1, emp.getEmployeeId());
                    ps.setString(2, emp.getFullName());
                    ps.setString(3, emp.getEmail());
                    ps.setString(4, emp.getDepartment());
                    ps.setString(5, emp.getRole().name());
                    ps.setDate(6, java.sql.Date.valueOf(emp.getHireDate()));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit(); // Commit Transaction
        } catch (SQLException e) {
            conn.rollback(); // Rollback on failure
            throw new SQLException("Transaction rolled back due to error: " + e.getMessage(), e);
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Loads domain records from MySQL database into the provided BankEngine.
     *
     * @param engine target BankEngine
     * @param conn   active JDBC Connection
     * @throws SQLException on database error
     * @throws BankingException on business rule violation
     */
    public synchronized void loadFromDatabase(BankEngine engine, Connection conn) throws SQLException, BankingException {
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Cannot load state: JDBC connection is null or closed.");
        }

        // 1. Load Customers
        String sqlCust = "SELECT customer_id, full_name, email, phone, address, created_at FROM customers";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlCust)) {
            while (rs.next()) {
                String cId = rs.getString("customer_id");
                String name = rs.getString("full_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                String address = rs.getString("address");
                engine.registerCustomer(cId, name, email, phone, address);
            }
        }

        // 2. Load Accounts
        String sqlAcc = "SELECT account_number, customer_id, account_type, balance, interest_rate, overdraft_limit, status, opened_at FROM accounts";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlAcc)) {
            while (rs.next()) {
                String accNum = rs.getString("account_number");
                String cId = rs.getString("customer_id");
                String type = rs.getString("account_type");
                double bal = rs.getDouble("balance");
                double rate = rs.getDouble("interest_rate");
                double overdraft = rs.getDouble("overdraft_limit");
                String statusStr = rs.getString("status");

                if ("SAVINGS".equalsIgnoreCase(type)) {
                    SavingsAccount sa = engine.openSavingsAccount(accNum, cId, 0.0, rate);
                    // Adjust balance directly after opening
                    if (bal > 0) {
                        sa.deposit(bal, "Database snapshot balance reload");
                    }
                    sa.setStatus(Account.AccountStatus.valueOf(statusStr));
                } else if ("CHECKING".equalsIgnoreCase(type)) {
                    CheckingAccount ca = engine.openCheckingAccount(accNum, cId, 0.0, overdraft);
                    if (bal != 0) {
                        if (bal > 0) {
                            ca.deposit(bal, "Database snapshot balance reload");
                        } else {
                            ca.withdraw(-bal);
                        }
                    }
                    ca.setStatus(Account.AccountStatus.valueOf(statusStr));
                }
            }
        }

        // 3. Load Loans
        String sqlLoan = "SELECT loan_id, customer_id, principal_amount, interest_rate, term_months, balance_remaining, status FROM loans";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlLoan)) {
            while (rs.next()) {
                String loanId = rs.getString("loan_id");
                String cId = rs.getString("customer_id");
                double principal = rs.getDouble("principal_amount");
                double rate = rs.getDouble("interest_rate");
                int term = rs.getInt("term_months");
                double balRemaining = rs.getDouble("balance_remaining");
                String statusStr = rs.getString("status");

                Loan loan = engine.createLoan(loanId, cId, principal, rate, term);
                loan.setStatus(Loan.LoanStatus.valueOf(statusStr));
            }
        }

        // 4. Load Employees
        String sqlEmp = "SELECT employee_id, full_name, email, department, role FROM employees";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlEmp)) {
            while (rs.next()) {
                String empId = rs.getString("employee_id");
                String name = rs.getString("full_name");
                String email = rs.getString("email");
                String dept = rs.getString("department");
                String roleStr = rs.getString("role");
                engine.registerEmployee(empId, name, email, dept, Employee.EmployeeRole.valueOf(roleStr));
            }
        }
    }
}
