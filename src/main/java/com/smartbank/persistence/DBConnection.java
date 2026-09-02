package com.smartbank.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Standard JDBC Database Connection Factory.
 * Demonstrates the fundamental 5 steps of Java Database Connectivity (JDBC):
 * 1. Register the JDBC Driver class
 * 2. Establish Connection via DriverManager
 * 3. Create Statements / PreparedStatements
 * 4. Execute Queries & Process ResultSets
 * 5. Close Connection / Auto-close resources
 */
public class DBConnection {

    // Default Connection Parameters (configurable)
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB = "smart_bank_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "root"; // adjust to match your local MySQL root password

    private static final String DEFAULT_URL = String.format(
            "jdbc:mysql://%s:%s/%s?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            DEFAULT_HOST, DEFAULT_PORT, DEFAULT_DB);

    /**
     * Step 1 & Step 2: Loads MySQL Driver and establishes a Connection using default parameters.
     *
     * @return active JDBC Connection
     * @throws SQLException on database connection error
     */
    public static Connection getConnection() throws SQLException {
        try {
            // STEP 1: Register Driver
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver Class [" + DRIVER_CLASS + "] not found on classpath.", e);
        }

        // STEP 2: Establish Connection
        return DriverManager.getConnection(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASS);
    }

    /**
     * Overloaded method to establish Connection with custom credentials.
     *
     * @param url      JDBC connection URL
     * @param user     database username
     * @param password database password
     * @return active JDBC Connection
     * @throws SQLException on database connection error
     */
    public static Connection getConnection(String url, String user, String password) throws SQLException {
        try {
            // STEP 1: Register Driver
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver Class [" + DRIVER_CLASS + "] not found on classpath.", e);
        }

        // STEP 2: Establish Connection
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Standalone main test method to directly verify MySQL connection from the command line.
     */
    public static void main(String[] args) {
        System.out.println("Testing MySQL JDBC Connection via DBConnection.java...");
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("? Successfully connected to MySQL database!");
                System.out.println("  Database: " + conn.getCatalog());
                System.out.println("  Driver:   " + conn.getMetaData().getDriverName() + " v" + conn.getMetaData().getDriverVersion());
            }
        } catch (SQLException e) {
            System.err.println("? Connection failed: " + e.getMessage());
            System.out.println("\nNote: Make sure your MySQL Server is running on port 3306 with database 'smart_bank_db'.");
        }
    }
}
