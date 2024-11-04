package com.PayrollService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreatePayrollRequestDto {
    private Long employeeId;
    private Integer year;  // Year of the payroll
    private Integer month; // Month of the payroll (1-12)
    private BigDecimal overtimePay;
    private BigDecimal bonuses;
}