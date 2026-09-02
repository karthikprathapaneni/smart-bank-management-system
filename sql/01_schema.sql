-- ====================================================================
-- Smart Bank Management System (SBMS)
-- Database DDL Schema (3NF Compliant)
-- ====================================================================

CREATE DATABASE IF NOT EXISTS smart_bank_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_bank_db;

-- Drop child tables first to respect FK dependencies
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS loans;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS customers;

-- 1. Customers Table
CREATE TABLE customers (
    customer_id VARCHAR(36) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_customers PRIMARY KEY (customer_id)
) ENGINE=InnoDB;

-- 2. Accounts Table
CREATE TABLE accounts (
    account_number VARCHAR(20) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    account_type ENUM('SAVINGS', 'CHECKING') NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    interest_rate DECIMAL(5, 4) DEFAULT 0.0000,
    overdraft_limit DECIMAL(15, 2) DEFAULT 0.00,
    status ENUM('ACTIVE', 'SUSPENDED', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_accounts PRIMARY KEY (account_number),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_account_balance CHECK (
        (account_type = 'SAVINGS' AND balance >= 0.00) OR
        (account_type = 'CHECKING' AND balance >= -overdraft_limit)
    )
) ENGINE=InnoDB;

-- 3. Transactions Table (Append-only Ledger)
CREATE TABLE transactions (
    transaction_id VARCHAR(36) NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    transaction_type ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT', 'INTEREST', 'LOAN_PAYMENT') NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    source_account VARCHAR(20),
    destination_account VARCHAR(20),
    reference_note VARCHAR(255),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_number) REFERENCES accounts (account_number) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_trans_amount CHECK (amount > 0.00)
) ENGINE=InnoDB;

-- 4. Loans Table
CREATE TABLE loans (
    loan_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    principal_amount DECIMAL(15, 2) NOT NULL,
    interest_rate DECIMAL(5, 4) NOT NULL,
    term_months INT NOT NULL,
    balance_remaining DECIMAL(15, 2) NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'ACTIVE', 'PAID_OFF', 'DEFAULTED') NOT NULL DEFAULT 'ACTIVE',
    disbursed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_loans PRIMARY KEY (loan_id),
    CONSTRAINT fk_loans_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_loan_principal CHECK (principal_amount > 0.00),
    CONSTRAINT chk_loan_term CHECK (term_months > 0)
) ENGINE=InnoDB;

-- 5. Employees Table
CREATE TABLE employees (
    employee_id VARCHAR(36) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    department VARCHAR(50) NOT NULL,
    role ENUM('TELLER', 'LOAN_OFFICER', 'MANAGER', 'ADMINISTRATOR') NOT NULL,
    hire_date DATE NOT NULL,
    CONSTRAINT pk_employees PRIMARY KEY (employee_id)
) ENGINE=InnoDB;

-- Indexes for performance
CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_transactions_account_time ON transactions(account_number, timestamp);
CREATE INDEX idx_loans_customer ON loans(customer_id);
