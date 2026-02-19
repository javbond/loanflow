package com.loanflow.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ApplicationNumberGenerator {

    private static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy");
    private static final AtomicLong counter = new AtomicLong(System.currentTimeMillis() % 100000);

    /**
     * Generate loan application number
     * Format: LN-YYYY-NNNNNN (e.g., LN-2024-001234)
     */
    public String generateLoanNumber() {
        return String.format("LN-%s-%06d",
                LocalDate.now().format(YEAR_FORMAT),
                counter.incrementAndGet() % 1000000);
    }

    /**
     * Generate customer number
     * Format: CUS-YYYY-NNNNNN (e.g., CUS-2024-001234)
     */
    public String generateCustomerNumber() {
        return String.format("CUS-%s-%06d",
                LocalDate.now().format(YEAR_FORMAT),
                counter.incrementAndGet() % 1000000);
    }

    /**
     * Generate document reference number
     * Format: DOC-YYYY-NNNNNN (e.g., DOC-2024-001234)
     */
    public String generateDocumentNumber() {
        return String.format("DOC-%s-%06d",
                LocalDate.now().format(YEAR_FORMAT),
                counter.incrementAndGet() % 1000000);
    }

    /**
     * Generate disbursement reference number
     * Format: DIS-YYYY-NNNNNN (e.g., DIS-2024-001234)
     */
    public String generateDisbursementNumber() {
        return String.format("DIS-%s-%06d",
                LocalDate.now().format(YEAR_FORMAT),
                counter.incrementAndGet() % 1000000);
    }

    /**
     * Generate generic reference with custom prefix
     */
    public String generateReference(String prefix) {
        return String.format("%s-%s-%06d",
                prefix.toUpperCase(),
                LocalDate.now().format(YEAR_FORMAT),
                counter.incrementAndGet() % 1000000);
    }
}
