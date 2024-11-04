package com.ORsystem.Service;

import com.ORsystem.Model.Depclass;
import com.ORsystem.ORgdto.Depdto;
import com.ORsystem.Repository.DepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class Depservice {

    private final DepRepository departmentRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public Depservice(DepRepository departmentRepository, RestTemplate restTemplate) {
        this.departmentRepository = departmentRepository;
        this.restTemplate = restTemplate;
    }

    public List<Depdto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getDepartmentsWithEmployeeCount() {
        return departmentRepository.findAll().stream()
                .map(this::mapDepartmentWithEmployeeCount)
                .collect(Collectors.toList());
    }

    public Depdto getDepartmentById(Long id) {
        Depclass dept = findDepartmentById(id);
        return convertToDto(dept);
    }

    public Depdto updateDepartment(Depdto depDto) {
        Depclass dept = findDepartmentById(depDto.getId());
        updateDepartmentFields(dept, depDto);
        Depclass updatedDept = departmentRepository.save(dept);
        return convertToDto(updatedDept);
    }

    public Depdto addDepartment(Depdto departmentDTO) {
        Depclass dept = new Depclass();
        dept.setName(departmentDTO.getName());
        dept.setLocation(departmentDTO.getLocation());
        dept.setAbbreviatedName(departmentDTO.getAbbreviatedName());
        Depclass savedDept = departmentRepository.save(dept);
        return convertToDto(savedDept);
    }

    public void deleteDepartment(Long id) {
        findDepartmentById(id); // Ensure the department exists before deleting
        departmentRepository.deleteById(id);
    }

    private Depdto convertToDto(Depclass dept) {
        return new Depdto(dept.getId(), dept.getName(), dept.getLocation(), dept.getAbbreviatedName());
    }

    private Depclass findDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found for ID: " + id));
    }

    private void updateDepartmentFields(Depclass dept, Depdto depDto) {
        Optional.ofNullable(depDto.getAbbreviatedName()).ifPresent(dept::setAbbreviatedName);
        Optional.ofNullable(depDto.getLocation()).ifPresent(dept::setLocation);
        Optional.ofNullable(depDto.getName()).ifPresent(dept::setName);
    }

    private Map<String, Object> mapDepartmentWithEmployeeCount(Depclass dep) {
        Map<String, Object> depMap = new HashMap<>();
        depMap.put("department", dep);
        Long departmentId = dep.getId();
        String employeeServiceUrl = "lb://EMPLOYMENT/api/v1/employee/bydep/" + departmentId;
        Integer employeeCount = restTemplate.getForObject(employeeServiceUrl, Integer.class);
        depMap.put("employeeCount", employeeCount);
        return depMap;
    }
}