package com.loanflow.loan.mapper;

import com.loanflow.dto.request.LoanApplicationRequest;
import com.loanflow.dto.response.LoanApplicationResponse;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanType;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-19T20:22:23+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 20.0.1 (Oracle Corporation)"
)
@Component
public class LoanApplicationMapperImpl implements LoanApplicationMapper {

    @Override
    public LoanApplication toEntity(LoanApplicationRequest request) {
        if ( request == null ) {
            return null;
        }

        LoanApplication.LoanApplicationBuilder loanApplication = LoanApplication.builder();

        loanApplication.loanType( stringToLoanType( request.getLoanType() ) );
        loanApplication.customerId( request.getCustomerId() );
        loanApplication.requestedAmount( request.getRequestedAmount() );
        loanApplication.tenureMonths( request.getTenureMonths() );
        loanApplication.purpose( request.getPurpose() );
        loanApplication.branchCode( request.getBranchCode() );

        return loanApplication.build();
    }

    @Override
    public LoanApplicationResponse toResponse(LoanApplication entity) {
        if ( entity == null ) {
            return null;
        }

        LoanApplicationResponse.LoanApplicationResponseBuilder loanApplicationResponse = LoanApplicationResponse.builder();

        loanApplicationResponse.loanType( loanTypeToString( entity.getLoanType() ) );
        loanApplicationResponse.id( entity.getId() );
        loanApplicationResponse.applicationNumber( entity.getApplicationNumber() );
        loanApplicationResponse.customerId( entity.getCustomerId() );
        loanApplicationResponse.requestedAmount( entity.getRequestedAmount() );
        loanApplicationResponse.approvedAmount( entity.getApprovedAmount() );
        loanApplicationResponse.interestRate( entity.getInterestRate() );
        loanApplicationResponse.tenureMonths( entity.getTenureMonths() );
        loanApplicationResponse.emiAmount( entity.getEmiAmount() );
        loanApplicationResponse.purpose( entity.getPurpose() );
        loanApplicationResponse.branchCode( entity.getBranchCode() );
        if ( entity.getAssignedOfficer() != null ) {
            loanApplicationResponse.assignedOfficer( entity.getAssignedOfficer().toString() );
        }
        loanApplicationResponse.cibilScore( entity.getCibilScore() );
        loanApplicationResponse.riskCategory( entity.getRiskCategory() );
        loanApplicationResponse.processingFee( entity.getProcessingFee() );
        loanApplicationResponse.expectedDisbursementDate( entity.getExpectedDisbursementDate() );
        loanApplicationResponse.submittedAt( entity.getSubmittedAt() );
        loanApplicationResponse.createdAt( entity.getCreatedAt() );
        loanApplicationResponse.updatedAt( entity.getUpdatedAt() );

        loanApplicationResponse.status( entity.getStatus() != null ? entity.getStatus().name() : null );
        loanApplicationResponse.workflowStage( entity.getStatus() != null ? entity.getStatus().getDisplayName() : null );

        return loanApplicationResponse.build();
    }

    @Override
    public void updateEntity(LoanApplication entity, LoanApplicationRequest request) {
        if ( request == null ) {
            return;
        }

        if ( request.getCustomerId() != null ) {
            entity.setCustomerId( request.getCustomerId() );
        }
        if ( request.getLoanType() != null ) {
            entity.setLoanType( Enum.valueOf( LoanType.class, request.getLoanType() ) );
        }
        if ( request.getRequestedAmount() != null ) {
            entity.setRequestedAmount( request.getRequestedAmount() );
        }
        if ( request.getTenureMonths() != null ) {
            entity.setTenureMonths( request.getTenureMonths() );
        }
        if ( request.getPurpose() != null ) {
            entity.setPurpose( request.getPurpose() );
        }
        if ( request.getBranchCode() != null ) {
            entity.setBranchCode( request.getBranchCode() );
        }
    }
}
