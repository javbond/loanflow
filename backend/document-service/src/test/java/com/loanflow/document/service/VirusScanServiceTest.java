package com.loanflow.document.service;

import com.loanflow.document.domain.entity.Document;
import com.loanflow.document.mapper.DocumentMapper;
import com.loanflow.document.repository.DocumentRepository;
import com.loanflow.document.service.impl.DocumentServiceImpl;
import com.loanflow.document.service.impl.NoOpVirusScanService;
import com.loanflow.dto.request.DocumentUploadRequest;
import com.loanflow.dto.response.DocumentResponse;
import com.loanflow.util.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * US-020: Virus Scan Integration Tests
 * Tests for ClamAV virus scanning in the document upload pipeline.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Virus Scan Service Tests")
class VirusScanServiceTest {

    @Nested
    @DisplayName("Upload Pipeline with Virus Scan")
    @ExtendWith(MockitoExtension.class)
    class UploadPipelineTests {

        @Mock private DocumentRepository repository;
        @Mock private DocumentMapper mapper;
        @Mock private StorageService storageService;
        @Mock private VirusScanService virusScanService;

        @InjectMocks
        private DocumentServiceImpl service;

        @Test
        @DisplayName("Should upload clean file successfully after virus scan")
        void shouldUploadCleanFileSuccessfully() {
            // Given
            UUID applicationId = UUID.randomUUID();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "PDF content".getBytes());
            DocumentUploadRequest request = DocumentUploadRequest.builder()
                    .applicationId(applicationId)
                    .customerId(UUID.randomUUID())
                    .documentType("PAN_CARD")
                    .build();

            Document savedDoc = Document.builder()
                    .id("doc-123")
                    .documentNumber("DOC-2024-000001")
                    .applicationId(applicationId)
                    .build();

            when(virusScanService.scan(any())).thenReturn(VirusScanResult.clean());
            when(storageService.upload(any(MultipartFile.class), anyString())).thenReturn("path/to/file");
            when(repository.save(any(Document.class))).thenReturn(savedDoc);
            when(mapper.toResponse(any(Document.class))).thenReturn(
                    DocumentResponse.builder().id("doc-123").build());

            // When
            DocumentResponse result = service.upload(file, request);

            // Then
            assertThat(result).isNotNull();
            verify(virusScanService).scan(file);
            verify(storageService).upload(any(MultipartFile.class), anyString());
            verify(repository).save(any(Document.class));
        }

        @Test
        @DisplayName("Should reject infected file and prevent storage")
        void shouldRejectInfectedFile() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "infected.pdf", "application/pdf", "malicious content".getBytes());
            DocumentUploadRequest request = DocumentUploadRequest.builder()
                    .applicationId(UUID.randomUUID())
                    .documentType("PAN_CARD")
                    .build();

            when(virusScanService.scan(any()))
                    .thenReturn(VirusScanResult.infected("Eicar-Test-Signature"));

            // When/Then
            assertThatThrownBy(() -> service.upload(file, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo("VIRUS_DETECTED");
                        assertThat(bex.getMessage()).contains("virus detected");
                        assertThat(bex.getMessage()).contains("Eicar-Test-Signature");
                    });

            // Verify file was NOT stored in MinIO
            verifyNoInteractions(storageService);
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should reject upload when virus scan fails (fail-closed)")
        void shouldRejectWhenVirusScanFails() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "content".getBytes());
            DocumentUploadRequest request = DocumentUploadRequest.builder()
                    .applicationId(UUID.randomUUID())
                    .documentType("PAN_CARD")
                    .build();

            when(virusScanService.scan(any()))
                    .thenReturn(VirusScanResult.error("Connection refused to ClamAV"));

            // When/Then
            assertThatThrownBy(() -> service.upload(file, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo("VIRUS_SCAN_FAILED");
                        assertThat(bex.getMessage()).contains("Unable to verify file safety");
                    });

            // Verify file was NOT stored
            verifyNoInteractions(storageService);
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should not scan when file type is invalid")
        void shouldNotScanWhenFileTypeInvalid() {
            // Given
            MockMultipartFile exeFile = new MockMultipartFile(
                    "file", "malware.exe", "application/x-msdownload", "EXE content".getBytes());
            DocumentUploadRequest request = DocumentUploadRequest.builder()
                    .applicationId(UUID.randomUUID())
                    .documentType("PAN_CARD")
                    .build();

            // When/Then
            assertThatThrownBy(() -> service.upload(exeFile, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("file type");

            // Verify virus scan was NOT called (short-circuit before scan)
            verifyNoInteractions(virusScanService);
            verifyNoInteractions(storageService);
        }

        @Test
        @DisplayName("Should not scan when file exceeds size limit")
        void shouldNotScanWhenFileTooLarge() {
            // Given
            byte[] largeContent = new byte[15 * 1024 * 1024]; // 15MB
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file", "large.pdf", "application/pdf", largeContent);
            DocumentUploadRequest request = DocumentUploadRequest.builder()
                    .applicationId(UUID.randomUUID())
                    .documentType("PAN_CARD")
                    .build();

            // When/Then
            assertThatThrownBy(() -> service.upload(largeFile, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("size");

            // Verify virus scan was NOT called (short-circuit before scan)
            verifyNoInteractions(virusScanService);
        }
    }

    @Nested
    @DisplayName("NoOp Virus Scanner")
    class NoOpScannerTests {

        @Test
        @DisplayName("Should always return CLEAN in dev/test mode")
        void noOpScannerShouldAlwaysReturnClean() {
            // Given
            NoOpVirusScanService noOpService = new NoOpVirusScanService();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "content".getBytes());

            // When
            VirusScanResult result = noOpService.scan(file);

            // Then
            assertThat(result.isClean()).isTrue();
            assertThat(result.isInfected()).isFalse();
            assertThat(result.getStatus()).isEqualTo(VirusScanResult.Status.CLEAN);
        }
    }

    @Nested
    @DisplayName("VirusScanResult Factory Methods")
    class VirusScanResultTests {

        @Test
        @DisplayName("Should create correct result objects via factory methods")
        void virusScanResultFactoryMethods() {
            // Clean result
            VirusScanResult clean = VirusScanResult.clean();
            assertThat(clean.isClean()).isTrue();
            assertThat(clean.isInfected()).isFalse();
            assertThat(clean.getVirusName()).isNull();
            assertThat(clean.getErrorMessage()).isNull();

            // Infected result
            VirusScanResult infected = VirusScanResult.infected("EICAR-TEST");
            assertThat(infected.isClean()).isFalse();
            assertThat(infected.isInfected()).isTrue();
            assertThat(infected.getVirusName()).isEqualTo("EICAR-TEST");
            assertThat(infected.getStatus()).isEqualTo(VirusScanResult.Status.INFECTED);

            // Error result
            VirusScanResult error = VirusScanResult.error("Connection refused");
            assertThat(error.isClean()).isFalse();
            assertThat(error.isInfected()).isFalse();
            assertThat(error.getErrorMessage()).isEqualTo("Connection refused");
            assertThat(error.getStatus()).isEqualTo(VirusScanResult.Status.ERROR);
        }
    }
}
