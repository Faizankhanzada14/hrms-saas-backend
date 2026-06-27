package com.hrms.employee.dto;

import com.hrms.employee.entity.PostingUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class MasterDataDtos {

    @Getter
    @Setter
    public static class DepartmentRequest {
        @NotBlank(message = "Department name is required")
        private String name;
    }

    @Getter
    @Setter
    public static class DepartmentResponse {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    public static class DesignationRequest {
        @NotBlank(message = "Designation title is required")
        private String title;
    }

    @Getter
    @Setter
    public static class DesignationResponse {
        private Long id;
        private String title;
    }

    @Getter
    @Setter
    public static class PostingUnitRequest {
        @NotBlank(message = "Posting unit name is required")
        private String name;

        @NotNull(message = "Unit type is required")
        private PostingUnit.UnitType unitType;

        private String branchName;
    }

    @Getter
    @Setter
    public static class PostingUnitResponse {
        private Long id;
        private String name;
        private PostingUnit.UnitType unitType;
        private String branchName;
    }
}
