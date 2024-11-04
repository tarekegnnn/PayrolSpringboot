package com.PayrollService.Repository;

import com.PayrollService.Model.PayrollModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollRepository extends JpaRepository<PayrollModel, Long> {
    List<PayrollModel> findByYearAndMonth(int year, int month);
    List<PayrollModel> findByEmployeeId(Long employeeId);
}
