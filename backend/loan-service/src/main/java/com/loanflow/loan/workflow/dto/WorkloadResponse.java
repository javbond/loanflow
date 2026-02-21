package com.loanflow.loan.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for the officer workload dashboard endpoint.
 * Contains per-officer workload metrics and aggregate totals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkloadResponse {

    /**
     * List of individual officer workloads
     */
    private List<OfficerWorkload> officers;

    /**
     * Total number of active tasks across all officers
     */
    private int totalActiveTaskCount;
}
