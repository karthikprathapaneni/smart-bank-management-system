package com.smartbank.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain entity representing an authorized Bank Staff Member.
 */
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum EmployeeRole {
        TELLER,
        LOAN_OFFICER,
        MANAGER,
        ADMINISTRATOR
    }

    private final String employeeId;
    private String fullName;
    private String email;
    private String department;
    private EmployeeRole role;
    private final LocalDate hireDate;

    public Employee(String employeeId, String fullName, String email, String department, EmployeeRole role, LocalDate hireDate) {
        this.employeeId = Objects.requireNonNull(employeeId, "Employee ID cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "Full name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.department = Objects.requireNonNull(department, "Department cannot be null");
        this.role = Objects.requireNonNull(role, "Role cannot be null");
        this.hireDate = (hireDate != null) ? hireDate : LocalDate.now();
    }

    public Employee(String employeeId, String fullName, String email, String department, EmployeeRole role) {
        this(employeeId, fullName, email, department, role, LocalDate.now());
    }

    public String getEmployeeId() {
        return employeeId;
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

    public synchronized String getDepartment() {
        return department;
    }

    public synchronized void setDepartment(String department) {
        this.department = Objects.requireNonNull(department, "Department cannot be null");
    }

    public synchronized EmployeeRole getRole() {
        return role;
    }

    public synchronized void setRole(EmployeeRole role) {
        this.role = Objects.requireNonNull(role, "Role cannot be null");
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        return Objects.equals(employeeId, employee.employeeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId);
    }

    @Override
    public String toString() {
        return String.format("Employee[ID: %s, Name: %s, Role: %s, Dept: %s, Email: %s]",
                employeeId, fullName, role, department, email);
    }
}
