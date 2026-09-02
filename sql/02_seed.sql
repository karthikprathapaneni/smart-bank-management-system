-- ====================================================================
-- Smart Bank Management System (SBMS)
-- Baseline Seed Data
-- ====================================================================

USE smart_bank_db;

-- 1. Insert Sample Customers
INSERT INTO customers (customer_id, full_name, email, phone, address) VALUES
('CUST-1001', 'Alice Johnson', 'alice.johnson@example.com', '+1-555-0101', '100 Financial Way, Suite 400, New York, NY'),
('CUST-1002', 'Bob Smith', 'bob.smith@example.com', '+1-555-0102', '250 Enterprise Blvd, Chicago, IL'),
('CUST-1003', 'Charlie Davis', 'charlie.davis@example.com', '+1-555-0103', '742 Evergreen Terrace, Springfield, OR'),
('CUST-1004', 'Diana Prince', 'diana.prince@example.com', '+1-555-0104', '1407 Graymalkin Lane, Salem, MA');

-- 2. Insert Accounts (Savings & Checking)
INSERT INTO accounts (account_number, customer_id, account_type, balance, interest_rate, overdraft_limit, status) VALUES
('ACC-SAV-1001', 'CUST-1001', 'SAVINGS', 25000.00, 0.0450, 0.00, 'ACTIVE'),
('ACC-CHK-1002', 'CUST-1001', 'CHECKING', 5000.00, 0.0000, 1500.00, 'ACTIVE'),
('ACC-SAV-2001', 'CUST-1002', 'SAVINGS', 12500.00, 0.0400, 0.00, 'ACTIVE'),
('ACC-CHK-2002', 'CUST-1002', 'CHECKING', 3200.00, 0.0000, 1000.00, 'ACTIVE'),
('ACC-SAV-3001', 'CUST-1003', 'SAVINGS', 8900.00, 0.0380, 0.00, 'ACTIVE'),
('ACC-CHK-4001', 'CUST-1004', 'CHECKING', 15000.00, 0.0000, 2500.00, 'ACTIVE');

-- 3. Insert Initial Transactions
INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, balance_after, source_account, destination_account, reference_note) VALUES
('TXN-0001', 'ACC-SAV-1001', 'DEPOSIT', 25000.00, 25000.00, NULL, 'ACC-SAV-1001', 'Initial opening balance deposit'),
('TXN-0002', 'ACC-CHK-1002', 'DEPOSIT', 5000.00, 5000.00, NULL, 'ACC-CHK-1002', 'Initial opening balance deposit'),
('TXN-0003', 'ACC-SAV-2001', 'DEPOSIT', 12500.00, 12500.00, NULL, 'ACC-SAV-2001', 'Initial opening balance deposit'),
('TXN-0004', 'ACC-CHK-2002', 'DEPOSIT', 3200.00, 3200.00, NULL, 'ACC-CHK-2002', 'Initial opening balance deposit'),
('TXN-0005', 'ACC-SAV-3001', 'DEPOSIT', 8900.00, 8900.00, NULL, 'ACC-SAV-3001', 'Initial opening balance deposit'),
('TXN-0006', 'ACC-CHK-4001', 'DEPOSIT', 15000.00, 15000.00, NULL, 'ACC-CHK-4001', 'Initial opening balance deposit');

-- 4. Insert Sample Loans
INSERT INTO loans (loan_id, customer_id, principal_amount, interest_rate, term_months, balance_remaining, status) VALUES
('LOAN-9001', 'CUST-1001', 50000.00, 0.0650, 60, 42500.00, 'ACTIVE'),
('LOAN-9002', 'CUST-1002', 15000.00, 0.0720, 36, 11200.00, 'ACTIVE');

-- 5. Insert Sample Employees
INSERT INTO employees (employee_id, full_name, email, department, role, hire_date) VALUES
('EMP-001', 'Marcus Vance', 'marcus.vance@smartbank.internal', 'Operations', 'MANAGER', '2020-03-15'),
('EMP-002', 'Sarah Jenkins', 'sarah.jenkins@smartbank.internal', 'Retail Banking', 'TELLER', '2022-06-01'),
('EMP-003', 'David Kross', 'david.kross@smartbank.internal', 'Lending', 'LOAN_OFFICER', '2021-09-10'),
('EMP-004', 'Elena Rostova', 'elena.rostova@smartbank.internal', 'IT & Security', 'ADMINISTRATOR', '2019-01-10');
