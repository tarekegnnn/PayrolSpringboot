package com.PayrollService.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class PayrollResponseDto {
    private Long id;  // Unique payroll ID
    private Long employeeId;  // Foreign key linking to Employee
    private String employeeFirstName;  // Employee's first name
    private Integer year;  // Year of the payroll
    private Integer month;  // Month of the payroll (1-12)
    private BigDecimal pensionDeduction; // Pension deduction amount
    private BigDecimal taxDeduction; // Tax deduction amount
    private BigDecimal overtimeHours;  // Overtime hours worked
    private BigDecimal overtimePay;  // Overtime pay amount
    private BigDecimal bonuses;  // Bonuses included in the payroll
    private BigDecimal totalDeductions;  // Total deductions
    private BigDecimal grossPay;  // Total gross pay (before deductions)
    private BigDecimal netPay;  // Total net pay (after deductions)
    private String paymentStatus;  // Payment status (Paid, Pending, Rejected)
    private BigDecimal absentDeduction;
    private BigDecimal baseSalary;
}