package com.smartbank.gui;

import com.smartbank.engine.BankEngine;
import com.smartbank.exceptions.AccountNotFoundException;
import com.smartbank.exceptions.BankingException;
import com.smartbank.exceptions.InsufficientBalanceException;
import com.smartbank.exceptions.InvalidTransactionException;
import com.smartbank.model.*;
import com.smartbank.persistence.PersistenceManager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Production-grade Java AWT User Interface for the Smart Bank Management System (SBMS).
 * Adheres strictly to the Java Delegation Event Model (AWT components, layout managers,
 * and event listener architecture) with full live MySQL JDBC integration controls.
 */
public class SmartBankGUI extends Frame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final BankEngine engine;
    private final PersistenceManager persistenceManager;

    // Header & Summary UI
    private Label lblBankTitle;
    private Label lblTotalBalance;
    private Label lblTotalAccounts;
    private Label lblTotalCustomers;

    // Direct MySQL JDBC Connection Bar
    private TextField txtDbHost;
    private TextField txtDbPort;
    private TextField txtDbName;
    private TextField txtDbUser;
    private TextField txtDbPassword;
    private Button btnTestDbConn;
    private Button btnSaveToDb;
    private Button btnLoadFromDb;
    private Label lblDbStatus;

    // Direct Operations Panel
    private Choice choiceAccountSelect;
    private TextField txtAmount;
    private TextField txtReference;
    private Button btnDeposit;
    private Button btnWithdraw;
    private Button btnApplyInterest;
    private Button btnCheckBalance;

    // Transfer Panel
    private Choice choiceTransferSrc;
    private Choice choiceTransferDest;
    private TextField txtTransferAmount;
    private TextField txtTransferMemo;
    private Button btnExecuteTransfer;

    // Account Creation Panel
    private Choice choiceCustomerForAccount;
    private Choice choiceAccountType;
    private TextField txtNewAccNum;
    private TextField txtInitialDeposit;
    private TextField txtRateOrOverdraft;
    private Button btnCreateAccount;

    // Customer Registration Panel
    private TextField txtCustId;
    private TextField txtCustName;
    private TextField txtCustEmail;
    private TextField txtCustPhone;
    private TextField txtCustAddress;
    private Button btnRegisterCustomer;

    // Persistence & Snapshot Controls
    private Button btnSerializeSnapshot;
    private Button btnDeserializeSnapshot;
    private Button btnRefreshData;

    // Real-Time Audit Log Console
    private java.awt.List listLogConsole;

    public SmartBankGUI(BankEngine engine) {
        super("Smart Bank Management System (SBMS) - Enterprise Desktop Console (CSA09)");
        this.engine = engine;
        this.persistenceManager = new PersistenceManager();

        initializeComponents();
        layoutUI();
        registerEventHandlers();
        refreshAllChoicesAndSummary();
        logMessage("SUCCESS", "Smart Bank Management System initialized and ready.");
    }

    private void initializeComponents() {
        // Typography & Colors
        Font titleFont = new Font("SansSerif", Font.BOLD, 15);

        // Header
        lblBankTitle = new Label("SMART BANK MANAGEMENT SYSTEM - CSA09 ENTERPRISE", Label.CENTER);
        lblBankTitle.setFont(titleFont);
        lblBankTitle.setBackground(new Color(24, 43, 73));
        lblBankTitle.setForeground(Color.WHITE);

        lblTotalBalance = new Label("Total Assets: $0.00", Label.LEFT);
        lblTotalAccounts = new Label("Accounts: 0", Label.CENTER);
        lblTotalCustomers = new Label("Customers: 0", Label.RIGHT);

        // MySQL Connection Bar Components
        txtDbHost = new TextField("localhost", 9);
        txtDbPort = new TextField("3306", 4);
        txtDbName = new TextField("smart_bank_db", 10);
        txtDbUser = new TextField("root", 6);
        txtDbPassword = new TextField("", 8);
        txtDbPassword.setEchoChar('*');
        btnTestDbConn = new Button("Test JDBC Conn");
        btnSaveToDb = new Button("Sync to MySQL");
        btnLoadFromDb = new Button("Load from MySQL");
        lblDbStatus = new Label("[DB: Standby / In-Memory]", Label.LEFT);
        lblDbStatus.setForeground(new Color(0, 100, 0));

        // Operations
        choiceAccountSelect = new Choice();
        txtAmount = new TextField(10);
        txtReference = new TextField(12);
        btnDeposit = new Button("Deposit Funds");
        btnWithdraw = new Button("Withdraw Funds");
        btnApplyInterest = new Button("Apply Interest");
        btnCheckBalance = new Button("View Account Details");

        // Transfers
        choiceTransferSrc = new Choice();
        choiceTransferDest = new Choice();
        txtTransferAmount = new TextField(10);
        txtTransferMemo = new TextField(15);
        btnExecuteTransfer = new Button("Execute Deadlock-Free Transfer");
        btnExecuteTransfer.setBackground(new Color(46, 139, 87));
        btnExecuteTransfer.setForeground(Color.WHITE);

        // New Account
        choiceCustomerForAccount = new Choice();
        choiceAccountType = new Choice();
        choiceAccountType.add("SAVINGS");
        choiceAccountType.add("CHECKING");
        txtNewAccNum = new TextField(12);
        txtInitialDeposit = new TextField(8);
        txtRateOrOverdraft = new TextField(8);
        btnCreateAccount = new Button("Open Bank Account");

        // Customer Registration
        txtCustId = new TextField(10);
        txtCustName = new TextField(14);
        txtCustEmail = new TextField(14);
        txtCustPhone = new TextField(10);
        txtCustAddress = new TextField(14);
        btnRegisterCustomer = new Button("Register New Customer");

        // Persistence
        btnSerializeSnapshot = new Button("Save State (bank_data.ser)");
        btnDeserializeSnapshot = new Button("Load State (bank_data.ser)");
        btnRefreshData = new Button("Refresh Views");

        // Log Console
        listLogConsole = new java.awt.List(12, false);
        listLogConsole.setFont(new Font("Monospaced", Font.PLAIN, 11));
        listLogConsole.setBackground(new Color(245, 247, 250));
    }

    private void layoutUI() {
        setLayout(new BorderLayout(6, 6));
        setBackground(new Color(230, 235, 242));

        // 1. TOP HEADER & METRIC SUMMARY & MYSQL BAR
        Panel topContainer = new Panel(new BorderLayout());
        topContainer.add(lblBankTitle, BorderLayout.NORTH);

        Panel summaryBar = new Panel(new GridLayout(1, 3, 10, 0));
        summaryBar.setBackground(new Color(210, 220, 235));
        summaryBar.add(lblTotalBalance);
        summaryBar.add(lblTotalAccounts);
        summaryBar.add(lblTotalCustomers);
        topContainer.add(summaryBar, BorderLayout.CENTER);

        // MySQL DB Connection Panel
        Panel dbBar = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        dbBar.setBackground(new Color(220, 230, 245));
        dbBar.add(new Label("MySQL Host:"));
        dbBar.add(txtDbHost);
        dbBar.add(new Label("Port:"));
        dbBar.add(txtDbPort);
        dbBar.add(new Label("DB:"));
        dbBar.add(txtDbName);
        dbBar.add(new Label("User:"));
        dbBar.add(txtDbUser);
        dbBar.add(new Label("Pass:"));
        dbBar.add(txtDbPassword);
        dbBar.add(btnTestDbConn);
        dbBar.add(btnSaveToDb);
        dbBar.add(btnLoadFromDb);
        dbBar.add(lblDbStatus);
        topContainer.add(dbBar, BorderLayout.SOUTH);

        add(topContainer, BorderLayout.NORTH);

        // 2. CENTER WORKSPACE (Grid of Panels)
        Panel centerPanel = new Panel(new GridLayout(2, 2, 8, 8));

        // Section A: Direct Account Transactions
        Panel pnlDirectOps = new Panel(new BorderLayout(5, 5));
        pnlDirectOps.setBackground(Color.WHITE);
        Label lblSectionA = new Label("1. Direct Account Transactions", Label.LEFT);
        lblSectionA.setFont(new Font("SansSerif", Font.BOLD, 12));
        pnlDirectOps.add(lblSectionA, BorderLayout.NORTH);

        Panel pnlDirectInputs = new Panel(new GridLayout(3, 2, 5, 5));
        pnlDirectInputs.add(new Label("Select Account:"));
        pnlDirectInputs.add(choiceAccountSelect);
        pnlDirectInputs.add(new Label("Amount ($):"));
        pnlDirectInputs.add(txtAmount);
        pnlDirectInputs.add(new Label("Reference / Memo:"));
        pnlDirectInputs.add(txtReference);
        pnlDirectOps.add(pnlDirectInputs, BorderLayout.CENTER);

        Panel pnlDirectButtons = new Panel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        pnlDirectButtons.add(btnDeposit);
        pnlDirectButtons.add(btnWithdraw);
        pnlDirectButtons.add(btnApplyInterest);
        pnlDirectButtons.add(btnCheckBalance);
        pnlDirectOps.add(pnlDirectButtons, BorderLayout.SOUTH);
        centerPanel.add(pnlDirectOps);

        // Section B: Deadlock-Free Fund Transfer
        Panel pnlTransfer = new Panel(new BorderLayout(5, 5));
        pnlTransfer.setBackground(Color.WHITE);
        Label lblSectionB = new Label("2. Atomic Concurrency Transfers", Label.LEFT);
        lblSectionB.setFont(new Font("SansSerif", Font.BOLD, 12));
        pnlTransfer.add(lblSectionB, BorderLayout.NORTH);

        Panel pnlTransferInputs = new Panel(new GridLayout(4, 2, 5, 5));
        pnlTransferInputs.add(new Label("Source Account:"));
        pnlTransferInputs.add(choiceTransferSrc);
        pnlTransferInputs.add(new Label("Destination Account:"));
        pnlTransferInputs.add(choiceTransferDest);
        pnlTransferInputs.add(new Label("Transfer Amount ($):"));
        pnlTransferInputs.add(txtTransferAmount);
        pnlTransferInputs.add(new Label("Transfer Reference:"));
        pnlTransferInputs.add(txtTransferMemo);
        pnlTransfer.add(pnlTransferInputs, BorderLayout.CENTER);

        Panel pnlTransferButtons = new Panel(new FlowLayout(FlowLayout.CENTER));
        pnlTransferButtons.add(btnExecuteTransfer);
        pnlTransfer.add(pnlTransferButtons, BorderLayout.SOUTH);
        centerPanel.add(pnlTransfer);

        // Section C: Account Opening
        Panel pnlOpenAcc = new Panel(new BorderLayout(5, 5));
        pnlOpenAcc.setBackground(Color.WHITE);
        Label lblSectionC = new Label("3. Account Creation (Savings/Checking)", Label.LEFT);
        lblSectionC.setFont(new Font("SansSerif", Font.BOLD, 12));
        pnlOpenAcc.add(lblSectionC, BorderLayout.NORTH);

        Panel pnlOpenInputs = new Panel(new GridLayout(5, 2, 5, 4));
        pnlOpenInputs.add(new Label("Customer Owner:"));
        pnlOpenInputs.add(choiceCustomerForAccount);
        pnlOpenInputs.add(new Label("Account Type:"));
        pnlOpenInputs.add(choiceAccountType);
        pnlOpenInputs.add(new Label("Account Number:"));
        pnlOpenInputs.add(txtNewAccNum);
        pnlOpenInputs.add(new Label("Initial Deposit ($):"));
        pnlOpenInputs.add(txtInitialDeposit);
        pnlOpenInputs.add(new Label("Rate (0.05) / Overdraft ($):"));
        pnlOpenInputs.add(txtRateOrOverdraft);
        pnlOpenAcc.add(pnlOpenInputs, BorderLayout.CENTER);

        Panel pnlOpenButtons = new Panel(new FlowLayout(FlowLayout.CENTER));
        pnlOpenButtons.add(btnCreateAccount);
        pnlOpenAcc.add(pnlOpenButtons, BorderLayout.SOUTH);
        centerPanel.add(pnlOpenAcc);

        // Section D: Customer Registration & Persistence
        Panel pnlCustAndPersist = new Panel(new BorderLayout(5, 5));
        pnlCustAndPersist.setBackground(Color.WHITE);
        Label lblSectionD = new Label("4. Customer Registration & Persistence", Label.LEFT);
        lblSectionD.setFont(new Font("SansSerif", Font.BOLD, 12));
        pnlCustAndPersist.add(lblSectionD, BorderLayout.NORTH);

        Panel pnlCustInputs = new Panel(new GridLayout(5, 2, 5, 4));
        pnlCustInputs.add(new Label("Customer ID:"));
        pnlCustInputs.add(txtCustId);
        pnlCustInputs.add(new Label("Full Name:"));
        pnlCustInputs.add(txtCustName);
        pnlCustInputs.add(new Label("Email:"));
        pnlCustInputs.add(txtCustEmail);
        pnlCustInputs.add(new Label("Phone:"));
        pnlCustInputs.add(txtCustPhone);
        pnlCustInputs.add(new Label("Address:"));
        pnlCustInputs.add(txtCustAddress);
        pnlCustAndPersist.add(pnlCustInputs, BorderLayout.CENTER);

        Panel pnlPersistButtons = new Panel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        pnlPersistButtons.add(btnRegisterCustomer);
        pnlPersistButtons.add(btnSerializeSnapshot);
        pnlPersistButtons.add(btnDeserializeSnapshot);
        pnlPersistButtons.add(btnRefreshData);
        pnlCustAndPersist.add(pnlPersistButtons, BorderLayout.SOUTH);
        centerPanel.add(pnlCustAndPersist);

        add(centerPanel, BorderLayout.CENTER);

        // 3. BOTTOM AUDIT TRAIL / REAL-TIME LOG LIST
        Panel bottomPanel = new Panel(new BorderLayout(4, 4));
        Label lblLogTitle = new Label("Real-Time Audit Trail & System Event Console:", Label.LEFT);
        lblLogTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        bottomPanel.add(lblLogTitle, BorderLayout.NORTH);
        bottomPanel.add(listLogConsole, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        setSize(1080, 760);
        setLocationRelativeTo(null);
    }

    private void registerEventHandlers() {
        btnDeposit.addActionListener(this);
        btnWithdraw.addActionListener(this);
        btnApplyInterest.addActionListener(this);
        btnCheckBalance.addActionListener(this);
        btnExecuteTransfer.addActionListener(this);
        btnCreateAccount.addActionListener(this);
        btnRegisterCustomer.addActionListener(this);
        btnSerializeSnapshot.addActionListener(this);
        btnDeserializeSnapshot.addActionListener(this);
        btnRefreshData.addActionListener(this);

        // MySQL Event Listeners
        btnTestDbConn.addActionListener(this);
        btnSaveToDb.addActionListener(this);
        btnLoadFromDb.addActionListener(this);

        // Window closing adapter
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });
    }

    // =========================================================================
    // Delegation Event Model Listener
    // =========================================================================
    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        try {
            if (src == btnDeposit) {
                handleDeposit();
            } else if (src == btnWithdraw) {
                handleWithdraw();
            } else if (src == btnApplyInterest) {
                handleApplyInterest();
            } else if (src == btnCheckBalance) {
                handleCheckBalance();
            } else if (src == btnExecuteTransfer) {
                handleTransfer();
            } else if (src == btnCreateAccount) {
                handleCreateAccount();
            } else if (src == btnRegisterCustomer) {
                handleRegisterCustomer();
            } else if (src == btnSerializeSnapshot) {
                handleSerialize();
            } else if (src == btnDeserializeSnapshot) {
                handleDeserialize();
            } else if (src == btnRefreshData) {
                refreshAllChoicesAndSummary();
                logMessage("INFO", "Console state refreshed successfully.");
            } else if (src == btnTestDbConn) {
                handleTestDbConnection();
            } else if (src == btnSaveToDb) {
                handleSaveToDatabase();
            } else if (src == btnLoadFromDb) {
                handleLoadFromDatabase();
            }
        } catch (InsufficientBalanceException ex) {
            logMessage("INSUFFICIENT_FUNDS", ex.getMessage());
        } catch (AccountNotFoundException ex) {
            logMessage("ACCOUNT_NOT_FOUND", ex.getMessage());
        } catch (InvalidTransactionException ex) {
            logMessage("INVALID_TRANSACTION", ex.getMessage());
        } catch (BankingException ex) {
            logMessage("BANKING_ERROR", ex.getMessage());
        } catch (NumberFormatException ex) {
            logMessage("FORMAT_ERROR", "Invalid numeric value entered. Please verify amount and rate inputs.");
        } catch (Exception ex) {
            logMessage("UNEXPECTED_ERROR", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    // =========================================================================
    // Action Handlers
    // =========================================================================
    private void handleDeposit() throws BankingException, AccountNotFoundException, InvalidTransactionException {
        String accNum = choiceAccountSelect.getSelectedItem();
        if (accNum == null || accNum.isBlank()) {
            throw new InvalidTransactionException("No account selected.");
        }
        double amount = Double.parseDouble(txtAmount.getText().trim());
        String memo = txtReference.getText().trim();
        if (memo.isBlank()) {
            memo = "Direct Deposit";
        }

        engine.deposit(accNum, amount, memo);
        Account acc = engine.getAccount(accNum);
        persistenceManagerAppendAudit(new Transaction(accNum, Transaction.TransactionType.DEPOSIT, amount, acc.getBalance(), null, accNum, memo));
        logMessage("SUCCESS", String.format("Deposited $%,.2f into [%s]. New Balance: $%,.2f", amount, accNum, acc.getBalance()));
        txtAmount.setText("");
        txtReference.setText("");
        refreshSummaryOnly();
    }

    private void handleWithdraw() throws BankingException, AccountNotFoundException, InsufficientBalanceException, InvalidTransactionException {
        String accNum = choiceAccountSelect.getSelectedItem();
        if (accNum == null || accNum.isBlank()) {
            throw new InvalidTransactionException("No account selected.");
        }
        double amount = Double.parseDouble(txtAmount.getText().trim());
        String memo = txtReference.getText().trim();
        if (memo.isBlank()) {
            memo = "Direct Withdrawal";
        }

        engine.withdraw(accNum, amount, memo);
        Account acc = engine.getAccount(accNum);
        persistenceManagerAppendAudit(new Transaction(accNum, Transaction.TransactionType.WITHDRAWAL, amount, acc.getBalance(), accNum, null, memo));
        logMessage("SUCCESS", String.format("Withdrew $%,.2f from [%s]. New Balance: $%,.2f", amount, accNum, acc.getBalance()));
        txtAmount.setText("");
        txtReference.setText("");
        refreshSummaryOnly();
    }

    private void handleApplyInterest() throws BankingException, AccountNotFoundException, InvalidTransactionException {
        String accNum = choiceAccountSelect.getSelectedItem();
        if (accNum == null || accNum.isBlank()) {
            throw new InvalidTransactionException("No account selected.");
        }
        Account acc = engine.getAccount(accNum);
        if (acc instanceof SavingsAccount sa) {
            double interest = sa.applyInterest();
            persistenceManagerAppendAudit(new Transaction(accNum, Transaction.TransactionType.INTEREST, interest, sa.getBalance(), null, accNum, "Annual Interest Accrual"));
            logMessage("SUCCESS", String.format("Applied interest of $%,.2f to Savings Account [%s]. New Balance: $%,.2f", interest, accNum, sa.getBalance()));
            refreshSummaryOnly();
        } else {
            throw new InvalidTransactionException(String.format("Account [%s] is a CheckingAccount and does not earn savings interest.", accNum));
        }
    }

    private void handleCheckBalance() throws AccountNotFoundException, InvalidTransactionException {
        String accNum = choiceAccountSelect.getSelectedItem();
        if (accNum == null || accNum.isBlank()) {
            throw new InvalidTransactionException("No account selected.");
        }
        Account acc = engine.getAccount(accNum);
        Customer cust = engine.getCustomer(acc.getCustomerId());
        String ownerName = (cust != null) ? cust.getFullName() : "Unknown";
        logMessage("INFO", String.format("Account: %s | Type: %s | Owner: %s (ID: %s) | Balance: $%,.2f | Status: %s",
                acc.getAccountNumber(), acc.getAccountType(), ownerName, acc.getCustomerId(), acc.getBalance(), acc.getStatus()));
    }

    private void handleTransfer() throws BankingException, AccountNotFoundException, InsufficientBalanceException, InvalidTransactionException {
        String srcAcc = choiceTransferSrc.getSelectedItem();
        String destAcc = choiceTransferDest.getSelectedItem();
        if (srcAcc == null || destAcc == null) {
            throw new InvalidTransactionException("Source and destination accounts must be selected.");
        }
        double amount = Double.parseDouble(txtTransferAmount.getText().trim());
        String memo = txtTransferMemo.getText().trim();
        if (memo.isBlank()) {
            memo = "P2P Transfer";
        }

        engine.transferFunds(srcAcc, destAcc, amount, memo);
        Account src = engine.getAccount(srcAcc);
        Account dest = engine.getAccount(destAcc);

        persistenceManagerAppendAudit(new Transaction(srcAcc, Transaction.TransactionType.TRANSFER_OUT, amount, src.getBalance(), srcAcc, destAcc, memo));
        persistenceManagerAppendAudit(new Transaction(destAcc, Transaction.TransactionType.TRANSFER_IN, amount, dest.getBalance(), srcAcc, destAcc, memo));

        logMessage("SUCCESS", String.format("Transferred $%,.2f: [%s] (Bal: $%,.2f) -> [%s] (Bal: $%,.2f)",
                amount, srcAcc, src.getBalance(), destAcc, dest.getBalance()));
        txtTransferAmount.setText("");
        txtTransferMemo.setText("");
        refreshSummaryOnly();
    }

    private void handleCreateAccount() throws BankingException, InvalidTransactionException {
        String custId = choiceCustomerForAccount.getSelectedItem();
        if (custId == null || custId.isBlank()) {
            throw new InvalidTransactionException("Please select a customer owner.");
        }
        String accType = choiceAccountType.getSelectedItem();
        String accNum = txtNewAccNum.getText().trim();
        if (accNum.isBlank()) {
            throw new InvalidTransactionException("Account number cannot be blank.");
        }
        double initDep = txtInitialDeposit.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtInitialDeposit.getText().trim());
        double extraParam = txtRateOrOverdraft.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtRateOrOverdraft.getText().trim());

        if ("SAVINGS".equalsIgnoreCase(accType)) {
            engine.openSavingsAccount(accNum, custId, initDep, extraParam);
            logMessage("SUCCESS", String.format("Opened Savings Account [%s] for Customer [%s] with Rate %.2f%% and initial deposit $%,.2f",
                    accNum, custId, extraParam * 100, initDep));
        } else {
            engine.openCheckingAccount(accNum, custId, initDep, extraParam);
            logMessage("SUCCESS", String.format("Opened Checking Account [%s] for Customer [%s] with Overdraft Limit $%,.2f and initial deposit $%,.2f",
                    accNum, custId, extraParam, initDep));
        }

        txtNewAccNum.setText("");
        txtInitialDeposit.setText("");
        txtRateOrOverdraft.setText("");
        refreshAllChoicesAndSummary();
    }

    private void handleRegisterCustomer() throws InvalidTransactionException {
        String cId = txtCustId.getText().trim();
        String name = txtCustName.getText().trim();
        String email = txtCustEmail.getText().trim();
        String phone = txtCustPhone.getText().trim();
        String address = txtCustAddress.getText().trim();

        if (cId.isBlank() || name.isBlank() || email.isBlank() || phone.isBlank()) {
            throw new InvalidTransactionException("Customer ID, Name, Email, and Phone are mandatory fields.");
        }

        engine.registerCustomer(cId, name, email, phone, address);
        logMessage("SUCCESS", String.format("Registered new Customer [%s]: %s (%s)", cId, name, email));

        txtCustId.setText("");
        txtCustName.setText("");
        txtCustEmail.setText("");
        txtCustPhone.setText("");
        txtCustAddress.setText("");
        refreshAllChoicesAndSummary();
    }

    private void handleSerialize() {
        try {
            persistenceManager.serializeState(engine, "bank_data.ser");
            logMessage("SUCCESS", "Bank engine binary state successfully serialized to [bank_data.ser].");
        } catch (IOException ex) {
            logMessage("PERSISTENCE_ERROR", "Failed to serialize state: " + ex.getMessage());
        }
    }

    private void handleDeserialize() {
        try {
            BankEngine loaded = persistenceManager.deserializeState("bank_data.ser");
            engine.clear();
            for (Customer c : loaded.getAllCustomers().values()) {
                engine.registerCustomer(c.getCustomerId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getAddress());
            }
            for (Account a : loaded.getAllAccounts().values()) {
                if (a instanceof SavingsAccount sa) {
                    SavingsAccount restored = engine.openSavingsAccount(sa.getAccountNumber(), sa.getCustomerId(), sa.getBalance(), sa.getInterestRate());
                    restored.setStatus(sa.getStatus());
                } else if (a instanceof CheckingAccount ca) {
                    CheckingAccount restored = engine.openCheckingAccount(ca.getAccountNumber(), ca.getCustomerId(), ca.getBalance(), ca.getOverdraftLimit());
                    restored.setStatus(ca.getStatus());
                }
            }
            logMessage("SUCCESS", "Bank engine state successfully reloaded from [bank_data.ser].");
            refreshAllChoicesAndSummary();
        } catch (Exception ex) {
            logMessage("PERSISTENCE_ERROR", "Failed to deserialize state: " + ex.getMessage());
        }
    }

    // =========================================================================
    // Live MySQL Database JDBC Handlers
    // =========================================================================
    private String getDbJdbcUrl() {
        String host = txtDbHost.getText().trim().isEmpty() ? "localhost" : txtDbHost.getText().trim();
        String port = txtDbPort.getText().trim().isEmpty() ? "3306" : txtDbPort.getText().trim();
        String db = txtDbName.getText().trim().isEmpty() ? "smart_bank_db" : txtDbName.getText().trim();
        return String.format("jdbc:mysql://%s:%s/%s?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, db);
    }

    private void handleTestDbConnection() {
        String url = getDbJdbcUrl();
        String user = txtDbUser.getText().trim();
        String pass = txtDbPassword.getText().trim();

        try (Connection conn = persistenceManager.getConnection(url, user, pass)) {
            DatabaseMetaData meta = conn.getMetaData();
            lblDbStatus.setText("[DB: Connected to MySQL]");
            lblDbStatus.setForeground(new Color(0, 120, 0));
            logMessage("MYSQL_SUCCESS", String.format("Connected to %s (%s) via JDBC driver %s",
                    meta.getDatabaseProductName(), meta.getDatabaseProductVersion(), meta.getDriverVersion()));
        } catch (SQLException ex) {
            lblDbStatus.setText("[DB: Connection Failed]");
            lblDbStatus.setForeground(Color.RED);
            logMessage("MYSQL_ERROR", "Failed to connect to MySQL: " + ex.getMessage());
        }
    }

    private void handleSaveToDatabase() {
        String url = getDbJdbcUrl();
        String user = txtDbUser.getText().trim();
        String pass = txtDbPassword.getText().trim();

        try (Connection conn = persistenceManager.getConnection(url, user, pass)) {
            persistenceManager.saveToDatabase(engine, conn);
            lblDbStatus.setText("[DB: Synchronized]");
            lblDbStatus.setForeground(new Color(0, 120, 0));
            logMessage("MYSQL_SUCCESS", String.format("Successfully saved %d Customers and %d Accounts into MySQL tables!",
                    engine.getAllCustomers().size(), engine.getAllAccounts().size()));
        } catch (SQLException ex) {
            lblDbStatus.setText("[DB: Sync Failed]");
            lblDbStatus.setForeground(Color.RED);
            logMessage("MYSQL_ERROR", "MySQL Save Error: " + ex.getMessage());
        }
    }

    private void handleLoadFromDatabase() {
        String url = getDbJdbcUrl();
        String user = txtDbUser.getText().trim();
        String pass = txtDbPassword.getText().trim();

        try (Connection conn = persistenceManager.getConnection(url, user, pass)) {
            engine.clear();
            persistenceManager.loadFromDatabase(engine, conn);
            lblDbStatus.setText("[DB: Loaded from MySQL]");
            lblDbStatus.setForeground(new Color(0, 120, 0));
            logMessage("MYSQL_SUCCESS", String.format("Loaded %d Customers and %d Accounts directly from MySQL!",
                    engine.getAllCustomers().size(), engine.getAllAccounts().size()));
            refreshAllChoicesAndSummary();
        } catch (Exception ex) {
            lblDbStatus.setText("[DB: Load Failed]");
            lblDbStatus.setForeground(Color.RED);
            logMessage("MYSQL_ERROR", "MySQL Load Error: " + ex.getMessage());
        }
    }

    private void persistenceManagerAppendAudit(Transaction txn) {
        try {
            persistenceManager.appendAuditLog(txn, "audit_log.txt");
        } catch (IOException e) {
            logMessage("AUDIT_WARN", "Could not write to audit_log.txt: " + e.getMessage());
        }
    }

    // =========================================================================
    // UI Helpers
    // =========================================================================
    private void refreshSummaryOnly() {
        double totalBalance = engine.calculateTotalBankBalance();
        lblTotalBalance.setText(String.format("Total Assets: $%,.2f", totalBalance));
        lblTotalAccounts.setText(String.format("Accounts: %d", engine.getAllAccounts().size()));
        lblTotalCustomers.setText(String.format("Customers: %d", engine.getAllCustomers().size()));
    }

    private void refreshAllChoicesAndSummary() {
        refreshSummaryOnly();

        // Update Account Choices
        choiceAccountSelect.removeAll();
        choiceTransferSrc.removeAll();
        choiceTransferDest.removeAll();
        for (String accNum : engine.getAllAccounts().keySet()) {
            choiceAccountSelect.add(accNum);
            choiceTransferSrc.add(accNum);
            choiceTransferDest.add(accNum);
        }

        // Update Customer Choices
        choiceCustomerForAccount.removeAll();
        for (String cId : engine.getAllCustomers().keySet()) {
            choiceCustomerForAccount.add(cId);
        }
    }

    private void logMessage(String level, String msg) {
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        String formatted = String.format("[%s] [%-14s] %s", timestamp, level, msg);
        listLogConsole.add(formatted, 0); // Prepend to top
        if (listLogConsole.getItemCount() > 200) {
            listLogConsole.remove(200);
        }
    }
}
