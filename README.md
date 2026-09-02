# Smart Bank Management System (SBMS) - CSA09

Production-grade, enterprise-ready banking management system implemented in Java 17+ matching specification CSA09.

## Architecture & Package Hierarchy

```
d:/smart-bank-management-system/
??? pom.xml                                -> Standard Maven Project Configuration
??? build_and_run.ps1                      -> Automated Build & Test Runner
??? sql/
?   ??? 01_schema.sql                      -> 3NF Normalized MySQL DDL with constraints
?   ??? 02_seed.sql                        -> Baseline Seed Data (Customers, Accounts, Loans, Staff)
??? src/
    ??? main/java/com/smartbank/
    ?   ??? Main.java                      -> Application bootstrap & EDT AWT launcher
    ?   ??? exceptions/                    -> Checked domain exceptions
    ?   ?   ??? BankingException.java
    ?   ?   ??? InsufficientBalanceException.java
    ?   ?   ??? AccountNotFoundException.java
    ?   ?   ??? InvalidTransactionException.java
    ?   ??? interfaces/                    -> Domain behavioral contracts
    ?   ?   ??? BankOperations.java
    ?   ??? model/                         -> Encapsulated business entities
    ?   ?   ??? Account.java (abstract)
    ?   ?   ??? SavingsAccount.java
    ?   ?   ??? CheckingAccount.java
    ?   ?   ??? Customer.java
    ?   ?   ??? Loan.java
    ?   ?   ??? Employee.java
    ?   ?   ??? Transaction.java
    ?   ??? engine/                        -> In-memory core & thread orchestration
    ?   ?   ??? BankEngine.java
    ?   ??? persistence/                   -> Multi-tier persistence (JDBC, binary, flat file)
    ?   ?   ??? PersistenceManager.java
    ?   ??? gui/                           -> Native Java AWT User Interface
    ?       ??? SmartBankGUI.java
    ??? test/java/com/smartbank/test/
        ??? BankEngineConcurrencyTest.java -> 10-thread deadlock-free stress validation
        ??? BankOperationsTest.java        -> OOP, polymorphism, & persistence unit tests
```

---

## Technical Highlights & Invariants

1. **OOP & Clean Code**:
   - Encapsulation with `private`/`protected` fields and thread-safe synchronized methods.
   - Abstract `Account` base class with polymorphism in concrete `SavingsAccount` (interest calculation, minimum balance) and `CheckingAccount` (overdraft protection).
   - Method overloading: `deposit(double amount)` and `deposit(double amount, String reference)`.
   - Generic utility method: `<T> Iterator<T> getEntityIterator(Collection<T> collection)`.

2. **Concurrency & Deadlock-Free Transfers**:
   - `BankEngine.transferFunds(...)` imposes **Total Natural Lock Ordering** using lexical comparison (`srcAccNum.compareTo(destAccNum)`) on synchronized monitors, eliminating circular-wait deadlock conditions.
   - Strict **Conservation of Total Balance Invariant**: `sum(initialBalances) == sum(finalBalances)`.

3. **Multi-tier Persistence**:
   - **Java Binary Object Serialization**: `serializeState()` & `deserializeState()` targeting `bank_data.ser`.
   - **Flat File I/O**: Append-only transactional audit trail via `BufferedWriter` to `audit_log.txt`.
   - **Relational JDBC**: PreparedStatements for all CRUD entities against MySQL with `setAutoCommit(false)` atomic transaction rollback protection.

4. **Desktop GUI (Java AWT)**:
   - Built entirely with standard AWT components (`Frame`, `Panel`, `Button`, `Choice`, `TextField`, `List`) and layout managers (`BorderLayout`, `GridLayout`, `FlowLayout`).
   - Follows the Java Delegation Event Model (`ActionListener`, `WindowAdapter`) and executes safely on the Event Dispatch Thread (`EventQueue.invokeLater`).

---

## Build & Test Instructions

### 1. Run Automated Test Suite & Concurrency Stress Test
```powershell
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Action test
```

### 2. Launch Native Java AWT Desktop GUI Console
```powershell
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Action run
```

### 3. Compile with Maven
```powershell
mvn clean test
```
