package com.loanflow.document.service;

import lombok.Builder;
import lombok.Data;

/**
 * Result of a virus scan operation.
 */
@Data
@Builder
public class VirusScanResult {

    public enum Status {
        CLEAN,
        INFECTED,
        ERROR
    }

    private Status status;
    private String virusName;    // populated when INFECTED
    private String errorMessage; // populated when ERROR

    public boolean isClean() {
        return status == Status.CLEAN;
    }

    public boolean isInfected() {
        return status == Status.INFECTED;
    }

    public static VirusScanResult clean() {
        return VirusScanResult.builder().status(Status.CLEAN).build();
    }

    public static VirusScanResult infected(String virusName) {
        return VirusScanResult.builder()
                .status(Status.INFECTED)
                .virusName(virusName)
                .build();
    }

    public static VirusScanResult error(String message) {
        return VirusScanResult.builder()
                .status(Status.ERROR)
                .errorMessage(message)
                .build();
    }
}
