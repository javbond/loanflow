package com.loanflow.loan.incomeverification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for income verification integration.
 * Mapped from application.yml: loanflow.income-verification.*
 */
@Configuration
@ConfigurationProperties(prefix = "loanflow.income-verification")
@Data
public class IncomeVerificationProperties {

    /** Income verification API base URL */
    private String baseUrl = "https://uat-api.incometax.gov.in/v1";

    /** API call timeout in milliseconds */
    private int timeoutMs = 30000;

    /** Maximum retry attempts for API calls */
    private int maxRetries = 3;

    /** Initial retry delay in milliseconds (doubles with each retry) */
    private long retryDelayMs = 1000;

    /** Redis cache TTL in hours for income verification responses */
    private long cacheTtlHours = 48;

    /** Whether income verification is enabled */
    private boolean enabled = true;
}
