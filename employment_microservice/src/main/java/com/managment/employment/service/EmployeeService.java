package com.managment.employment.service;

import com.managment.employment.dto.Departmentdto;
import com.managment.employment.model.Employee;
import com.managment.employment.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class EmployeeService {

    private final RestTemplate restTemplate;  // Use RestTemplate
    private final EmployeeRepository employeeRepository;

    public Employee addEmployee(Employee employee) {
        Departmentdto department = fetchDepartment(employee.getDepartmentId());

        // Validate the department and set it in the employee
        if (department != null) {
            employee.setDepartmentId(department.getId());
            return employeeRepository.save(employee);
        } else {
            throw new RuntimeException("Department not found for ID: " + employee.getDepartmentId());
        }
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public int getEmployeesByDepartment(Long departmentId) {
        return employeeRepository.countByDepartmentId(departmentId);
    }

    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    public Employee updateEmployee(Employee employee) {
        // Check if the employee exists before updating
        if (!employeeRepository.existsById(employee.getId())) {
            throw new RuntimeException("Employee not found for ID: " + employee.getId());
        }
        return employeeRepository.save(employee);
    }

    public boolean deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found for ID: " + id);
        }
        employeeRepository.deleteById(id);
        return true;
    }

    private Departmentdto fetchDepartment(Long departmentId) {
        String departmentServiceUrl = "lb://ORGANIZATION/api/v1/org/departments/" + departmentId;
        return restTemplate.getForObject(departmentServiceUrl, Departmentdto.class);
    }
}