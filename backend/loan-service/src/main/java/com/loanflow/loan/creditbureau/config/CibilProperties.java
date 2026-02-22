package com.loanflow.loan.creditbureau.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for CIBIL credit bureau integration.
 * Mapped from application.yml: loanflow.cibil.*
 */
@Configuration
@ConfigurationProperties(prefix = "loanflow.cibil")
@Data
public class CibilProperties {

    /** CIBIL API base URL */
    private String baseUrl = "https://uat-api.cibil.com/v2";

    /** CIBIL member code */
    private String memberCode = "LOANFLOW-DEV";

    /** CIBIL security code */
    private String securityCode = "dev-secret";

    /** API call timeout in milliseconds */
    private int timeoutMs = 30000;

    /** Maximum retry attempts for API calls */
    private int maxRetries = 3;

    /** Initial retry delay in milliseconds (doubles with each retry) */
    private long retryDelayMs = 1000;

    /** Redis cache TTL in hours for bureau responses */
    private long cacheTtlHours = 24;

    /** Whether CIBIL integration is enabled */
    private boolean enabled = true;
}
