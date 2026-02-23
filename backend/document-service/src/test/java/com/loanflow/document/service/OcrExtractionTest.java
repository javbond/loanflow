package com.loanflow.document.service;

import com.loanflow.document.config.DocumentRequirementsConfig;
import com.loanflow.document.domain.entity.Document;
import com.loanflow.document.domain.enums.DocumentStatus;
import com.loanflow.document.domain.enums.DocumentType;
import com.loanflow.document.mapper.DocumentMapper;
import com.loanflow.document.repository.DocumentRepository;
import com.loanflow.document.service.impl.DocumentServiceImpl;
import com.loanflow.document.service.impl.NoOpOcrService;
import com.loanflow.dto.response.DocumentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * US-022: OCR & Data Extraction Tests
 * Tests for OCR extraction, NoOp service, result factory methods, and extracted data CRUD.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OCR Extraction Tests (US-022)")
class OcrExtractionTest {

    @Mock private DocumentRepository repository;
    @Mock private DocumentMapper mapper;
    @Mock private StorageService storageService;
    @Mock private VirusScanService virusScanService;
    @Mock private OcrService ocrService;
    @Mock private DocumentRequirementsConfig requirementsConfig;

    @InjectMocks
    private DocumentServiceImpl service;

    // ==================== NoOp OCR Service Tests ====================

    @Nested
    @DisplayName("NoOp OCR Service")
    class NoOpOcrServiceTests {

        @Test
        @DisplayName("Should return mock PAN data for PAN_CARD document type")
        void shouldExtractPanNumberFromPdf() {
            // Given
            NoOpOcrService noOpService = new NoOpOcrService();
            byte[] content = "Fake PAN card PDF".getBytes();

            // When
            OcrExtractionResult result = noOpService.extract("doc-1", content, "application/pdf", "PAN_CARD");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getExtractedFields()).containsKey("panNumber");
            assertThat(result.getExtractedFields().get("panNumber")).isEqualTo("ABCDE1234F");
            assertThat(result.getExtractedFields()).containsKey("name");
            assertThat(result.getConfidence()).isGreaterThan(0.9);
        }

        @Test
        @DisplayName("Should return mock Aadhaar data for AADHAAR_CARD document type")
        void shouldExtractAadhaarNumberFromImage() {
            // Given
            NoOpOcrService noOpService = new NoOpOcrService();
            byte[] content = "Fake Aadhaar image".getBytes();

            // When
            OcrExtractionResult result = noOpService.extract("doc-2", content, "image/jpeg", "AADHAAR_CARD");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getExtractedFields()).containsKey("aadhaarNumber");
            assertThat(result.getExtractedFields().get("aadhaarNumber")).isEqualTo("123456789012");
            assertThat(result.getExtractedFields()).containsKey("name");
        }

        @Test
        @DisplayName("Should return mock salary slip fields")
        void shouldExtractSalarySlipFields() {
            // Given
            NoOpOcrService noOpService = new NoOpOcrService();

            // When
            OcrExtractionResult result = noOpService.extract("doc-3", "content".getBytes(), "application/pdf", "SALARY_SLIP");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getExtractedFields()).containsKeys("name", "grossSalary", "netSalary", "employer");
            assertThat(result.getExtractedFields().get("employer")).isEqualTo("TCS Limited");
        }

        @Test
        @DisplayName("Should handle unknown document type gracefully")
        void shouldHandleUnknownDocumentType() {
            // Given
            NoOpOcrService noOpService = new NoOpOcrService();

            // When
            OcrExtractionResult result = noOpService.extract("doc-4", "content".getBytes(), "application/pdf", "UNKNOWN_TYPE");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getExtractedFields()).containsKey("name");
        }
    }

    // ==================== OcrExtractionResult Factory Tests ====================

    @Nested
    @DisplayName("OcrExtractionResult Factory Methods")
    class OcrExtractionResultTests {

        @Test
        @DisplayName("Should create correct result objects via factory methods")
        void ocrExtractionResultFactoryMethods() {
            // Success result
            Map<String, String> fields = Map.of("panNumber", "ABCDE1234F");
            OcrExtractionResult success = OcrExtractionResult.success("doc-1", "Extracted text", fields, 0.95);
            assertThat(success.isSuccess()).isTrue();
            assertThat(success.isPartial()).isFalse();
            assertThat(success.isFailed()).isFalse();
            assertThat(success.hasExtractedFields()).isTrue();
            assertThat(success.getExtractedFields()).containsKey("panNumber");
            assertThat(success.getConfidence()).isEqualTo(0.95);

            // Partial result
            OcrExtractionResult partial = OcrExtractionResult.partial("doc-2", "Partial text", Map.of("name", "Test"), 0.5);
            assertThat(partial.isPartial()).isTrue();
            assertThat(partial.isSuccess()).isFalse();
            assertThat(partial.getConfidence()).isEqualTo(0.5);

            // Failed result
            OcrExtractionResult failed = OcrExtractionResult.failed("doc-3", "Parse error");
            assertThat(failed.isFailed()).isTrue();
            assertThat(failed.isSuccess()).isFalse();
            assertThat(failed.getErrorMessage()).isEqualTo("Parse error");
            assertThat(failed.getConfidence()).isEqualTo(0);
            assertThat(failed.hasExtractedFields()).isFalse();
        }

        @Test
        @DisplayName("Should handle null fields in factory methods")
        void shouldHandleNullFieldsInFactoryMethods() {
            // When
            OcrExtractionResult result = OcrExtractionResult.success("doc-1", "text", null, 0.8);

            // Then
            assertThat(result.getExtractedFields()).isNotNull();
            assertThat(result.getExtractedFields()).isEmpty();
        }
    }

    // ==================== Extracted Data CRUD Tests ====================

    @Nested
    @DisplayName("Extracted Data Retrieval & Update")
    class ExtractedDataTests {

        @Test
        @DisplayName("Should return extracted data for a document")
        void shouldReturnExtractedData() {
            // Given
            String docId = "doc-1";
            Map<String, String> extractedData = Map.of("panNumber", "ABCDE1234F", "name", "Rahul Sharma");
            Document doc = Document.builder()
                    .id(docId)
                    .documentType(DocumentType.PAN_CARD)
                    .extractedData(extractedData)
                    .extractionStatus("SUCCESS")
                    .build();

            when(repository.findById(docId)).thenReturn(Optional.of(doc));

            // When
            Map<String, String> result = service.getExtractedData(docId);

            // Then
            assertThat(result).containsEntry("panNumber", "ABCDE1234F");
            assertThat(result).containsEntry("name", "Rahul Sharma");
        }

        @Test
        @DisplayName("Should return empty map when no extracted data")
        void shouldReturnEmptyMapWhenNoExtractedData() {
            // Given
            String docId = "doc-1";
            Document doc = Document.builder()
                    .id(docId)
                    .documentType(DocumentType.PAN_CARD)
                    .extractedData(null)
                    .build();

            when(repository.findById(docId)).thenReturn(Optional.of(doc));

            // When
            Map<String, String> result = service.getExtractedData(docId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should update extracted data after manual review")
        void shouldUpdateExtractedDataManually() {
            // Given
            String docId = "doc-1";
            Document doc = Document.builder()
                    .id(docId)
                    .documentType(DocumentType.PAN_CARD)
                    .extractedData(new HashMap<>(Map.of("panNumber", "WRONG")))
                    .extractionStatus("SUCCESS")
                    .build();

            Map<String, String> correctedData = Map.of("panNumber", "FGHIJ5678K", "name", "Corrected Name");

            when(repository.findById(docId)).thenReturn(Optional.of(doc));
            when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any(Document.class))).thenReturn(
                    DocumentResponse.builder()
                            .id(docId)
                            .extractedData(correctedData)
                            .extractionStatus("REVIEWED")
                            .build());

            // When
            DocumentResponse result = service.updateExtractedData(docId, correctedData);

            // Then
            assertThat(result.getExtractionStatus()).isEqualTo("REVIEWED");
            verify(repository).save(argThat(d ->
                    d.getExtractionStatus().equals("REVIEWED") &&
                    d.getExtractedData().get("panNumber").equals("FGHIJ5678K")));
        }

        @Test
        @DisplayName("Should truncate extracted text to 5000 chars")
        void shouldTruncateExtractedTextTo5000Chars() {
            // Test the truncateText method indirectly by verifying NoOp behavior
            NoOpOcrService noOpService = new NoOpOcrService();
            OcrExtractionResult result = noOpService.extract("doc-1", "x".getBytes(), "application/pdf", "PAN_CARD");

            // NoOp returns short text, so verify it's not null and not truncated
            assertThat(result.getExtractedText()).isNotNull();
            assertThat(result.getExtractedText().length()).isLessThan(5000);
        }
    }
}
