package com.loanflow.loan.creditbureau.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Summary of a credit enquiry from CIBIL report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnquirySummary implements Serializable {
    private LocalDate enquiryDate;
    private String memberName;
    private String purpose;
    private double amount;
}
