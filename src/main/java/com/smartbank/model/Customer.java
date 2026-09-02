package com.smartbank.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Domain entity representing a Bank Customer with associated account references.
 */
public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String customerId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private final LocalDateTime createdAt;
    private final List<String> accountNumbers;

    public Customer(String customerId, String fullName, String email, String phone, String address, LocalDateTime createdAt) {
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "Full name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.phone = Objects.requireNonNull(phone, "Phone cannot be null");
        this.address = address;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.accountNumbers = new CopyOnWriteArrayList<>();
    }

    public Customer(String customerId, String fullName, String email, String phone, String address) {
        this(customerId, fullName, email, phone, address, LocalDateTime.now());
    }

    public synchronized String getCustomerId() {
        return customerId;
    }

    public synchronized String getFullName() {
        return fullName;
    }

    public synchronized void setFullName(String fullName) {
        this.fullName = Objects.requireNonNull(fullName, "Full name cannot be null");
    }

    public synchronized String getEmail() {
        return email;
    }

    public synchronized void setEmail(String email) {
        this.email = Objects.requireNonNull(email, "Email cannot be null");
    }

    public synchronized String getPhone() {
        return phone;
    }

    public synchronized void setPhone(String phone) {
        this.phone = Objects.requireNonNull(phone, "Phone cannot be null");
    }

    public synchronized String getAddress() {
        return address;
    }

    public synchronized void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void addAccountNumber(String accountNumber) {
        if (accountNumber != null && !accountNumbers.contains(accountNumber)) {
            accountNumbers.add(accountNumber);
        }
    }

    public void removeAccountNumber(String accountNumber) {
        accountNumbers.remove(accountNumber);
    }

    public List<String> getAccountNumbers() {
        return Collections.unmodifiableList(accountNumbers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public String toString() {
        return String.format("Customer[ID: %s, Name: %s, Email: %s, Accounts: %d]",
                customerId, fullName, email, accountNumbers.size());
    }
}
