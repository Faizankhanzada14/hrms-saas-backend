package com.hrms.employee.mapper;

import com.hrms.employee.dto.EmployeeResponse;
import com.hrms.employee.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeMapper {

    @Mapping(target = "fullName", expression = "java(employee.getFirstName() + \" \" + employee.getLastName())")
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "designationId", source = "designation.id")
    @Mapping(target = "designationTitle", source = "designation.title")
    @Mapping(target = "postingUnitId", source = "postingUnit.id")
    @Mapping(target = "postingUnitName", source = "postingUnit.name")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", expression = "java(employee.getManager() != null ? employee.getManager().getFirstName() + \" \" + employee.getManager().getLastName() : null)")
    EmployeeResponse toResponse(Employee employee);
}
