package com.Attendance.service;

import com.Attendance.dto.AttendanceDto;
import com.Attendance.dto.AttendanceWithNamesDto; // Import the new DTO
import com.Attendance.dto.Depdto;
import com.Attendance.dto.EmployeeDto;
import com.Attendance.modle.AttendanceModel;
import com.Attendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final RestTemplate restTemplate;
    private final AttendanceRepository attendanceRepository;

    @Autowired
    public AttendanceService(RestTemplate restTemplate, AttendanceRepository attendanceRepository) {
        this.restTemplate = restTemplate;
        this.attendanceRepository = attendanceRepository;
    }

    public List<AttendanceWithNamesDto> getAllAttendanceRecords() {
        List<AttendanceModel> attendanceRecords = attendanceRepository.findAll();
        return attendanceRecords.stream()
                .map(this::convertToAttendanceWithNamesDto)
                .collect(Collectors.toList());
    }

    public Optional<AttendanceModel> getAttendanceById(Long id) {
        return attendanceRepository.findById(id);
    }

    public AttendanceModel createAttendance(AttendanceDto attendanceDto) {
        validateEmployeeAndDepartment(attendanceDto);
        AttendanceModel attendance = new AttendanceModel();
        populateAttendanceFields(attendance, attendanceDto);
        return attendanceRepository.save(attendance);
    }

    public List<AttendanceWithNamesDto> findAttendanceByMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return attendanceRepository.findAll().stream()
                .filter(a -> !a.getDate().isBefore(startDate) && !a.getDate().isAfter(endDate))
                .map(this::convertToAttendanceWithNamesDto)
                .collect(Collectors.toList());

    }

    public List<AttendanceModel> findEachAttendanceByMonth(int empId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return attendanceRepository.findByEmployeeId(Long.valueOf(empId)).stream()
                .filter(a -> !a.getDate().isBefore(startDate) && !a.getDate().isAfter(endDate))
                .collect(Collectors.toList());
    }

    public AttendanceModel createOrUpdateAttendance(AttendanceDto attendanceDto) {
        AttendanceModel attendance = attendanceRepository.findByEmployeeIdAndDate(
                        attendanceDto.getEmployeeId(), attendanceDto.getDate())
                .orElse(new AttendanceModel());

        populateAttendanceFields(attendance, attendanceDto);
        return attendanceRepository.save(attendance);
    }

    public void deleteAttendance(Long id) {
        attendanceRepository.deleteById(id);
    }

    private void validateEmployeeAndDepartment(AttendanceDto attendanceDto) {
        String employeeUrl = "lb://EMPLOYMENT/api/v1/employee/" + attendanceDto.getEmployeeId();
        String departmentUrl = "lb://ORGANIZATION/api/v1/org/departments/" + attendanceDto.getDepartmentId();

        try {
            EmployeeDto employee = restTemplate.getForObject(employeeUrl, EmployeeDto.class);
            if (employee == null) {
                throw new RuntimeException("Employee not found with ID: " + attendanceDto.getEmployeeId());
            }

            Depdto department = restTemplate.getForObject(departmentUrl, Depdto.class);
            if (department == null) {
                throw new RuntimeException("Department not found with ID: " + attendanceDto.getDepartmentId());
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Error fetching employee or department details: " + e.getMessage(), e);
        }
    }

    private void populateAttendanceFields(AttendanceModel attendance, AttendanceDto attendanceDto) {
        attendance.setEmployeeId(attendanceDto.getEmployeeId());
        attendance.setDepartmentId(attendanceDto.getDepartmentId());
        attendance.setStatus(attendanceDto.getStatus().name());
        attendance.setDate(attendanceDto.getDate());
    }

    private AttendanceWithNamesDto convertToAttendanceWithNamesDto(AttendanceModel attendance) {
        EmployeeDto employee = restTemplate.getForObject("lb://EMPLOYMENT/api/v1/employee/" + attendance.getEmployeeId(), EmployeeDto.class);
        Depdto department = restTemplate.getForObject("lb://ORGANIZATION/api/v1/org/departments/" + attendance.getDepartmentId(), Depdto.class);

        return new AttendanceWithNamesDto(
                attendance.getId(),
                attendance.getEmployeeId(),
                employee != null ? employee.getFirstName() : "Unknown",
                attendance.getDepartmentId(),
                department != null ? department.getName() : "Unknown",
                attendance.getStatus(),
                attendance.getDate()
        );
    }
}