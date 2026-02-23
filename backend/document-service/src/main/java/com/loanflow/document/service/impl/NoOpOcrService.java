package com.loanflow.document.service.impl;

import com.loanflow.document.service.OcrExtractionResult;
import com.loanflow.document.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * No-op OCR service for dev/test environments (US-022).
 * Returns mock extracted data based on document type — no Tika required.
 */
@Service
@Profile({"dev", "test", "default"})
@Slf4j
public class NoOpOcrService implements OcrService {

    @Override
    public OcrExtractionResult extract(String documentId, byte[] content, String contentType, String documentType) {
        log.debug("NoOp OCR: returning mock extraction for document {} (type: {})", documentId, documentType);

        Map<String, String> mockFields = getMockFields(documentType);

        return OcrExtractionResult.success(
                documentId,
                "Mock extracted text for " + documentType,
                mockFields,
                0.95
        );
    }

    private Map<String, String> getMockFields(String documentType) {
        Map<String, String> fields = new HashMap<>();

        switch (documentType) {
            case "PAN_CARD" -> {
                fields.put("panNumber", "ABCDE1234F");
                fields.put("name", "Rahul Sharma");
            }
            case "AADHAAR_CARD" -> {
                fields.put("aadhaarNumber", "123456789012");
                fields.put("name", "Rahul Sharma");
            }
            case "SALARY_SLIP" -> {
                fields.put("name", "Rahul Sharma");
                fields.put("grossSalary", "75000");
                fields.put("netSalary", "62000");
                fields.put("employer", "TCS Limited");
            }
            case "BANK_STATEMENT" -> {
                fields.put("name", "Rahul Sharma");
                fields.put("accountNumber", "1234567890123");
                fields.put("ifscCode", "SBIN0001234");
            }
            default -> {
                fields.put("name", "Rahul Sharma");
            }
        }

        return fields;
    }
}
