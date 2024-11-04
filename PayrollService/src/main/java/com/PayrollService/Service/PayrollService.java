package com.PayrollService.Service;

import com.PayrollService.Dto.*;
import com.PayrollService.Exceptions.BadRequestException;
import com.PayrollService.Model.PayrollModel;
import com.PayrollService.Model.PayrollResponseDto;
import com.PayrollService.Repository.PayrollRepository;
import com.PayrollService.utils.PayroleCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PayrollService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PayrollRepository payrollRepository;

    private final PayroleCalculator calculator = new PayroleCalculator();

    public PayrollModel createPayroll(CreatePayrollRequestDto requestDto) {
        validateEmployeeId(requestDto.getEmployeeId());
        checkForExistingPayroll(requestDto.getEmployeeId(), requestDto.getYear(), requestDto.getMonth());

        Employeedto employee = fetchEmployeeOrThrow(requestDto.getEmployeeId());
        PayrollModel payroll = initializePayroll(requestDto, employee);

        // Set the default payment status to PENDING
        payroll.setPaymentStatus(PayrollModel.PaymentStatus.PENDING);
        payroll.setCreatedAt(LocalDateTime.now());
        payroll.setUpdatedAt(LocalDateTime.now());


        // Fetch deductions and attendance
        List<Deductions> deductions = fetchDeductions("lb://ORGANIZATION/api/v1/org/deductions");
        List<AttendanceDto> attendance = fetchAttendance("lb://ATTENDANCE/api/v1/attendance/" + employee.getId() + "/" + requestDto.getYear() + "/" + requestDto.getMonth());

        // Calculate payroll components
        calculatePayrollComponents(payroll, employee, deductions, attendance);
        return payrollRepository.save(payroll);
    }

    public List<PayrollResponseDto> getPayrollsByYearAndMonth(int year, int month) {
        List<PayrollModel> payrolls = payrollRepository.findByYearAndMonth(year, month);
        List<PayrollResponseDto> responseDtos = new ArrayList<>();

        for (PayrollModel payroll : payrolls) {
            Employeedto employee = fetchEmployeeOrThrow(payroll.getEmployeeId());

            PayrollResponseDto dto = new PayrollResponseDto();
            dto.setId(payroll.getId());
            dto.setEmployeeId(payroll.getEmployeeId());
            dto.setEmployeeFirstName(employee.getFirstName());
            dto.setBaseSalary(BigDecimal.valueOf(employee.getSalary()));
            dto.setYear(payroll.getYear());
            dto.setMonth(payroll.getMonth());
            dto.setPensionDeduction(payroll.getPensionDeduction());
            dto.setTaxDeduction(payroll.getTaxDeduction());
            dto.setOvertimeHours(payroll.getOvertimeHours());
            dto.setOvertimePay(payroll.getOvertimePay());
            dto.setBonuses(payroll.getBonuses());
            dto.setTotalDeductions(payroll.getTotalDeductions());
            dto.setGrossPay(payroll.getGrossPay());
            dto.setNetPay(payroll.getNetPay());
            dto.setPaymentStatus(payroll.getPaymentStatus() != null ? payroll.getPaymentStatus().name() : "UNKNOWN");
            dto.setAbsentDeduction(payroll.getAbsentDeduction());

            responseDtos.add(dto);
        }

        return responseDtos;
    }

    public void deletePayrollsByYearAndMonth(int year, int month) {
        List<PayrollModel> payrolls = payrollRepository.findByYearAndMonth(year, month);
        if (payrolls.isEmpty()) {
            throw new BadRequestException("No payrolls found for the year " + year + " and month " + month);
        }
        payrollRepository.deleteAll(payrolls);
    }

    public List<PayrollModel> createPayrollForAllEmployees(int year, int month) {
        List<Employeedto> allEmployees = fetchAllEmployees();
        List<PayrollModel> payrolls = new ArrayList<>();

        for (Employeedto employee : allEmployees) {
            CreatePayrollRequestDto requestDto = new CreatePayrollRequestDto();
            requestDto.setEmployeeId(employee.getId());
            requestDto.setYear(year);
            requestDto.setMonth(month);
            requestDto.setOvertimePay(BigDecimal.ZERO);
            requestDto.setBonuses(BigDecimal.ZERO);


            PayrollModel payroll = createPayroll(requestDto);
            payrolls.add(payroll);
        }
        return payrolls;
    }

    private List<Employeedto> fetchAllEmployees() {
        String employeesUrl = "lb://EMPLOYMENT/api/v1/employee";
        ResponseEntity<List<Employeedto>> response = restTemplate.exchange(
                employeesUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Employeedto>>() {}
        );
        return response.getBody();
    }

    private PayrollModel initializePayroll(CreatePayrollRequestDto requestDto, Employeedto employee) {
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(employee.getId());
        payroll.setYear(requestDto.getYear());
        payroll.setMonth(requestDto.getMonth());
        payroll.setOvertimePay(requestDto.getOvertimePay());
        payroll.setBonuses(requestDto.getBonuses());
        return payroll;
    }

    private void calculatePayrollComponents(PayrollModel payroll, Employeedto employee, List<Deductions> deductions, List<AttendanceDto> attendance) {
        double baseSalary = Optional.ofNullable(employee.getSalary()).orElse(0.0);
        double absentDeduction = calculator.getAbsentDeduction(attendance, baseSalary);
        payroll.setAbsentDeduction(BigDecimal.valueOf(absentDeduction));

        DeductionResultDTO deductionResult = calculator.calculateDeductions(baseSalary, deductions);
        double incomeTax = calculator.calculateIncomeTax(baseSalary);
        double pensionTax = calculator.calculatePensionContribution(baseSalary, "public").doubleValue();

        BigDecimal netPay = calculateNetPay(baseSalary, incomeTax, pensionTax, deductionResult.getTotalDeductions(), absentDeduction);
        populatePayrollFields(payroll, baseSalary, incomeTax, pensionTax, deductionResult, netPay);
        payroll.setUpdatedAt(LocalDateTime.now());
    }

    private void checkForExistingPayroll(Long employeeId, int year, int month) {
        List<PayrollModel> existingPayrolls = payrollRepository.findByEmployeeId(employeeId);
        for (PayrollModel existingPayroll : existingPayrolls) {
            if (existingPayroll.getYear() == year && existingPayroll.getMonth() == month) {
                throw new BadRequestException("Payroll already exists for employee ID: " + employeeId +
                        " for the year and month: " + year + "-" + month);
            }
        }
    }

    private void validateEmployeeId(Long employeeId) {
        if (employeeId == null) {
            throw new BadRequestException("Employee ID must not be null.");
        }
    }

    private Employeedto fetchEmployeeOrThrow(Long employeeId) {
        String employeeServiceUrl = "lb://EMPLOYMENT/api/v1/employee/" + employeeId;
        try {
            Employeedto employee = restTemplate.getForObject(employeeServiceUrl, Employeedto.class);
            if (employee == null) {
                throw new BadRequestException("Employee not found for ID: " + employeeId);
            }
            return employee;
        } catch (HttpClientErrorException e) {
            handleEmployeeFetchError(employeeId, e);
            return null; // Unreachable, but required for compilation
        }
    }

    private List<Deductions> fetchDeductions(String deductionUrl) {
        ResponseEntity<List<Deductions>> responseEntity = restTemplate.exchange(
                deductionUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Deductions>>() {}
        );
        return responseEntity.getBody();
    }

    private List<AttendanceDto> fetchAttendance(String attendanceUrl) {
        ResponseEntity<List<AttendanceDto>> responseEntity = restTemplate.exchange(
                attendanceUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<AttendanceDto>>() {}
        );
        return responseEntity.getBody();
    }

    private void handleEmployeeFetchError(Long employeeId, HttpClientErrorException e) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new BadRequestException("Employee not found for ID: " + employeeId);
        }
        throw new RuntimeException("An error occurred while retrieving the employee: " + e.getMessage());
    }

    private BigDecimal calculateNetPay(double baseSalary, double incomeTax, double pensionTax, double totalDeduction, double absentDeduction) {
        return BigDecimal.valueOf(baseSalary)
                .subtract(BigDecimal.valueOf(incomeTax + pensionTax + absentDeduction + totalDeduction));
    }

    private void populatePayrollFields(PayrollModel payroll, double baseSalary, double incomeTax, double pensionTax, DeductionResultDTO deductionResult, BigDecimal netPay) {
        payroll.setNetPay(netPay);
        payroll.setPensionDeduction(BigDecimal.valueOf(pensionTax));
        payroll.setTaxDeduction(BigDecimal.valueOf(incomeTax));
        payroll.setDeductionDetails(deductionResult.getDeductionList());
        payroll.setTotalDeductions(BigDecimal.valueOf(incomeTax + pensionTax + deductionResult.getTotalDeductions() + payroll.getAbsentDeduction().doubleValue()));
        payroll.setGrossPay(calculateGrossPay(baseSalary, payroll));
    }

    private BigDecimal calculateGrossPay(double baseSalary, PayrollModel payroll) {
        return BigDecimal.valueOf(calculator.claculategrosspay(baseSalary,
                payroll.getOvertimePay().doubleValue(),
                payroll.getBonuses().doubleValue()));
    }

    public PayrollModel getPayrollById(Long id) {
        return payrollRepository.findById(id).orElseThrow(() -> new RuntimeException("Payroll not found with ID: " + id));
    }

    public List<PayrollModel> getAllPayrolls() {
        return payrollRepository.findAll();
    }

    public List<PayrollModel> getPayrollsByEmployeeId(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId);
    }

    public PayrollModel updatePayroll(Long id, PayrollModel payrollDetails) {
        PayrollModel existingPayroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found with ID: " + id));

        updateExistingPayroll(existingPayroll, payrollDetails);
        return payrollRepository.save(existingPayroll);
    }

    private void updateExistingPayroll(PayrollModel existingPayroll, PayrollModel payrollDetails) {
        existingPayroll.setOvertimePay(payrollDetails.getOvertimePay());
        existingPayroll.setBonuses(payrollDetails.getBonuses());
        existingPayroll.setGrossPay(payrollDetails.getGrossPay());
        existingPayroll.setNetPay(payrollDetails.getNetPay());

        // Update payment status only if it's not null
        if (payrollDetails.getPaymentStatus() != null) {
            existingPayroll.setPaymentStatus(payrollDetails.getPaymentStatus());
        }
        existingPayroll.setUpdatedAt(LocalDateTime.now());
    }

    public void deletePayroll(Long id) {
        if (!payrollRepository.existsById(id)) {
            throw new BadRequestException("Payroll not found with ID: " + id);
        }
        payrollRepository.deleteById(id);
    }
}