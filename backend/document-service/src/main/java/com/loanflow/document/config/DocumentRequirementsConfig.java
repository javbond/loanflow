package com.loanflow.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for required documents per loan type (US-021).
 * Loaded from application.yml under loanflow.documents.required.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "loanflow.documents")
public class DocumentRequirementsConfig {

    private Map<String, List<String>> required = new HashMap<>();

    /**
     * Get required document types for a given loan type.
     * Returns empty list if loan type is not configured.
     */
    public List<String> getRequiredDocuments(String loanType) {
        return required.getOrDefault(loanType, List.of());
    }

    /**
     * Check if a loan type has configured document requirements.
     */
    public boolean hasRequirements(String loanType) {
        return required.containsKey(loanType);
    }
}
