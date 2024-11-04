package com.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceWithNamesDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long departmentId;
    private String departmentName;
    private String status;
    private LocalDate date;

}