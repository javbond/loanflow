package com.loanflow.loan.creditbureau.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * REST request body for manual credit bureau pull.
 * Either applicationId or pan must be provided.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditPullRequest {
    private UUID applicationId;
    private String pan;
}
