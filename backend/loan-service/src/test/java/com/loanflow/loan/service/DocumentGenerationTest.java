package com.loanflow.loan.service;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.domain.enums.LoanType;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.service.impl.ThymeleafDocumentGenerationService;
import com.loanflow.util.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * US-023: Document Generation Tests
 * Tests for sanction letter PDF generation via Thymeleaf + Flying Saucer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Document Generation Tests (US-023)")
class DocumentGenerationTest {

    @Mock
    private LoanApplicationRepository repository;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private ThymeleafDocumentGenerationService service;

    private UUID applicationId;
    private LoanApplication approvedLoan;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        approvedLoan = LoanApplication.builder()
                .id(applicationId)
                .applicationNumber("LN-2026-000001")
                .customerId(UUID.randomUUID())
                .customerEmail("rahul.sharma@example.com")
                .loanType(LoanType.HOME_LOAN)
                .requestedAmount(new BigDecimal("5000000"))
                .approvedAmount(new BigDecimal("4500000"))
                .interestRate(new BigDecimal("8.50"))
                .tenureMonths(240)
                .emiAmount(new BigDecimal("38985.42"))
                .processingFee(new BigDecimal("11250"))
                .purpose("Purchase of residential property")
                .cibilScore(752)
                .branchCode("MUM001")
                .status(LoanStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("Should generate sanction letter PDF for approved loan")
    void shouldGenerateSanctionLetterPdf() {
        // Given
        when(repository.findById(applicationId)).thenReturn(Optional.of(approvedLoan));
        // Return valid XHTML that Flying Saucer can render
        String mockHtml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>Sanction Letter</title></head>
                <body><p>Sanction Letter for LN-2026-000001</p></body>
                </html>
                """;
        when(templateEngine.process(eq("sanction-letter"), any(Context.class))).thenReturn(mockHtml);

        // When
        byte[] pdf = service.generateSanctionLetter(applicationId);

        // Then
        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        // PDF files start with %PDF magic bytes
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF");
    }

    @Test
    @DisplayName("Should populate all loan fields in template context")
    void shouldPopulateAllLoanFields() {
        // Given
        when(repository.findById(applicationId)).thenReturn(Optional.of(approvedLoan));
        String mockHtml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>Test</title></head>
                <body><p>Test</p></body>
                </html>
                """;
        when(templateEngine.process(eq("sanction-letter"), any(Context.class))).thenReturn(mockHtml);

        // When
        service.generateSanctionLetter(applicationId);

        // Then - verify template was called with correct context variables
        verify(templateEngine).process(eq("sanction-letter"), argThat(ctx -> {
            Context context = (Context) ctx;
            return "LN-2026-000001".equals(context.getVariable("applicationNumber")) &&
                   "rahul.sharma@example.com".equals(context.getVariable("customerName")) &&
                   "Home Loan".equals(context.getVariable("loanType")) &&
                   new BigDecimal("4500000").equals(context.getVariable("approvedAmount")) &&
                   new BigDecimal("8.50").equals(context.getVariable("interestRate")) &&
                   Integer.valueOf(240).equals(context.getVariable("tenureMonths")) &&
                   new BigDecimal("38985.42").equals(context.getVariable("emiAmount")) &&
                   new BigDecimal("11250").equals(context.getVariable("processingFee")) &&
                   "Purchase of residential property".equals(context.getVariable("purpose")) &&
                   Integer.valueOf(752).equals(context.getVariable("cibilScore")) &&
                   "MUM001".equals(context.getVariable("branchCode"));
        }));
    }

    @Test
    @DisplayName("Should reject sanction letter generation for non-approved loan")
    void shouldRejectForNonApprovedLoan() {
        // Given
        LoanApplication draftLoan = LoanApplication.builder()
                .id(applicationId)
                .status(LoanStatus.DRAFT)
                .loanType(LoanType.PERSONAL_LOAN)
                .build();
        when(repository.findById(applicationId)).thenReturn(Optional.of(draftLoan));

        // When/Then
        assertThatThrownBy(() -> service.generateSanctionLetter(applicationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED or DISBURSED");
    }

    @Test
    @DisplayName("Should handle missing customer name gracefully (use email as fallback)")
    void shouldHandleMissingCustomerName() {
        // Given
        approvedLoan.setCustomerEmail(null); // no email
        when(repository.findById(applicationId)).thenReturn(Optional.of(approvedLoan));
        String mockHtml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>Test</title></head>
                <body><p>Test</p></body>
                </html>
                """;
        when(templateEngine.process(eq("sanction-letter"), any(Context.class))).thenReturn(mockHtml);

        // When
        byte[] pdf = service.generateSanctionLetter(applicationId);

        // Then — should not throw; template handles null via ?: operator
        assertThat(pdf).isNotNull();
        verify(templateEngine).process(eq("sanction-letter"), argThat(ctx -> {
            Context context = (Context) ctx;
            // customerName is set to email (null in this case), template handles it
            return context.getVariable("customerName") == null;
        }));
    }

    @Test
    @DisplayName("Should return valid PDF bytes for approved loan")
    void shouldReturnValidPdfBytes() {
        // Given
        when(repository.findById(applicationId)).thenReturn(Optional.of(approvedLoan));
        String mockHtml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>Sanction Letter</title></head>
                <body>
                <h1>LOANFLOW</h1>
                <p>Loan Sanction Letter</p>
                <p>Amount: 4,500,000.00</p>
                </body>
                </html>
                """;
        when(templateEngine.process(eq("sanction-letter"), any(Context.class))).thenReturn(mockHtml);

        // When
        byte[] pdf = service.generateSanctionLetter(applicationId);

        // Then
        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(100); // PDF should be non-trivially sized
    }

    @Test
    @DisplayName("Should generate sanction letter for DISBURSED loan too")
    void shouldGenerateForDisbursedLoan() {
        // Given
        approvedLoan.setStatus(LoanStatus.DISBURSED);
        when(repository.findById(applicationId)).thenReturn(Optional.of(approvedLoan));
        String mockHtml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>Test</title></head>
                <body><p>Test</p></body>
                </html>
                """;
        when(templateEngine.process(eq("sanction-letter"), any(Context.class))).thenReturn(mockHtml);

        // When
        byte[] pdf = service.generateSanctionLetter(applicationId);

        // Then — DISBURSED is also an allowed status
        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent loan")
    void shouldThrowForNonExistentLoan() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.generateSanctionLetter(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LoanApplication");
    }
}
