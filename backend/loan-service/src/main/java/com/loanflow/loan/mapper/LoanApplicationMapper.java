package com.loanflow.loan.mapper;

import com.loanflow.dto.request.LoanApplicationRequest;
import com.loanflow.dto.response.LoanApplicationResponse;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanType;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LoanApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applicationNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "loanType", source = "loanType", qualifiedByName = "stringToLoanType")
    @Mapping(target = "approvedAmount", ignore = true)
    @Mapping(target = "interestRate", ignore = true)
    @Mapping(target = "emiAmount", ignore = true)
    @Mapping(target = "cibilScore", ignore = true)
    @Mapping(target = "riskCategory", ignore = true)
    @Mapping(target = "processingFee", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "workflowInstanceId", ignore = true)
    @Mapping(target = "assignedOfficer", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "disbursedAt", ignore = true)
    @Mapping(target = "expectedDisbursementDate", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    LoanApplication toEntity(LoanApplicationRequest request);

    @Mapping(target = "loanType", source = "loanType", qualifiedByName = "loanTypeToString")
    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? entity.getStatus().name() : null)")
    @Mapping(target = "workflowStage", expression = "java(entity.getStatus() != null ? entity.getStatus().getDisplayName() : null)")
    @Mapping(target = "customerName", ignore = true) // Fetched from customer-service
    @Mapping(target = "documents", ignore = true) // Fetched from document-service
    @Mapping(target = "workflowHistory", ignore = true) // Fetched from workflow
    LoanApplicationResponse toResponse(LoanApplication entity);

    @Named("stringToLoanType")
    default LoanType stringToLoanType(String loanType) {
        if (loanType == null) {
            return null;
        }
        return LoanType.valueOf(loanType.toUpperCase());
    }

    @Named("loanTypeToString")
    default String loanTypeToString(LoanType loanType) {
        if (loanType == null) {
            return null;
        }
        return loanType.name();
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget LoanApplication entity, LoanApplicationRequest request);
}
