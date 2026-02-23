package com.loanflow.document.service.impl;

import com.loanflow.document.service.OcrExtractionResult;
import com.loanflow.document.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Apache Tika-based OCR extraction service (US-022).
 * Active in UAT/PROD profiles. Extracts text from PDF/image documents
 * and applies regex patterns to identify structured data (PAN, Aadhaar, etc.).
 */
@Service
@Profile({"uat", "prod"})
@Slf4j
public class TikaOcrService implements OcrService {

    // Indian PAN: 5 uppercase letters + 4 digits + 1 uppercase letter (e.g., ABCDE1234F)
    private static final Pattern PAN_PATTERN = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");

    // Indian Aadhaar: 12 digits, optionally grouped with spaces (e.g., 1234 5678 9012)
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\d{4}\\s?\\d{4}\\s?\\d{4}");

    // IFSC Code: 4 uppercase letters + 0 + 6 alphanumeric (e.g., SBIN0001234)
    private static final Pattern IFSC_PATTERN = Pattern.compile("[A-Z]{4}0[A-Z0-9]{6}");

    // Amount patterns: Indian currency (e.g., Rs. 50,000 or INR 1,00,000.00)
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)");

    // Name pattern (basic — after "Name:" or "Employee Name:")
    private static final Pattern NAME_PATTERN = Pattern.compile("(?:Name|Employee\\s*Name)\\s*:?\\s*([A-Z][a-zA-Z\\s]{2,40})");

    private final AutoDetectParser parser = new AutoDetectParser();

    @Override
    public OcrExtractionResult extract(String documentId, byte[] content, String contentType, String documentType) {
        log.info("Extracting text from document {} (type: {}, contentType: {})", documentId, documentType, contentType);

        try {
            // Extract raw text using Tika
            BodyContentHandler handler = new BodyContentHandler(100_000); // 100KB max text
            Metadata metadata = new Metadata();
            metadata.set(Metadata.CONTENT_TYPE, contentType);
            ParseContext context = new ParseContext();

            parser.parse(new ByteArrayInputStream(content), handler, metadata, context);

            String extractedText = handler.toString().trim();

            if (extractedText.isEmpty()) {
                log.warn("No text extracted from document {}", documentId);
                return OcrExtractionResult.partial(documentId, "", new HashMap<>(), 0.0);
            }

            // Apply document-type-specific extraction
            Map<String, String> fields = extractFieldsByType(extractedText, documentType);
            double confidence = calculateConfidence(fields, documentType);

            OcrExtractionResult.Status status = fields.isEmpty()
                    ? OcrExtractionResult.Status.PARTIAL
                    : OcrExtractionResult.Status.SUCCESS;

            log.info("Extraction complete for {}: {} fields, confidence {}", documentId, fields.size(), confidence);

            return OcrExtractionResult.builder()
                    .documentId(documentId)
                    .extractedText(extractedText)
                    .extractedFields(fields)
                    .confidence(confidence)
                    .status(status)
                    .build();

        } catch (Exception e) {
            log.error("OCR extraction failed for document {}: {}", documentId, e.getMessage(), e);
            return OcrExtractionResult.failed(documentId, e.getMessage());
        }
    }

    private Map<String, String> extractFieldsByType(String text, String documentType) {
        Map<String, String> fields = new HashMap<>();

        switch (documentType) {
            case "PAN_CARD" -> extractPanFields(text, fields);
            case "AADHAAR_CARD" -> extractAadhaarFields(text, fields);
            case "SALARY_SLIP" -> extractSalarySlipFields(text, fields);
            case "BANK_STATEMENT" -> extractBankStatementFields(text, fields);
            default -> extractGenericFields(text, fields);
        }

        return fields;
    }

    private void extractPanFields(String text, Map<String, String> fields) {
        Matcher matcher = PAN_PATTERN.matcher(text);
        if (matcher.find()) {
            fields.put("panNumber", matcher.group());
        }
        extractNameField(text, fields);
    }

    private void extractAadhaarFields(String text, Map<String, String> fields) {
        Matcher matcher = AADHAAR_PATTERN.matcher(text);
        if (matcher.find()) {
            String aadhaar = matcher.group().replaceAll("\\s", "");
            fields.put("aadhaarNumber", aadhaar);
        }
        extractNameField(text, fields);
    }

    private void extractSalarySlipFields(String text, Map<String, String> fields) {
        extractNameField(text, fields);

        // Try to find gross and net salary amounts
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(text);
        int amountCount = 0;
        while (amountMatcher.find() && amountCount < 3) {
            String amount = amountMatcher.group(1).replaceAll(",", "");
            switch (amountCount) {
                case 0 -> fields.put("grossSalary", amount);
                case 1 -> fields.put("netSalary", amount);
                case 2 -> fields.put("deductions", amount);
            }
            amountCount++;
        }

        // Try to find employer name
        Pattern employerPattern = Pattern.compile("(?:Company|Employer|Organization)\\s*:?\\s*([A-Za-z][A-Za-z\\s&.]{2,50})");
        Matcher empMatcher = employerPattern.matcher(text);
        if (empMatcher.find()) {
            fields.put("employer", empMatcher.group(1).trim());
        }
    }

    private void extractBankStatementFields(String text, Map<String, String> fields) {
        extractNameField(text, fields);

        // Account number (10-18 digits)
        Pattern acctPattern = Pattern.compile("(?:Account\\s*(?:No\\.?|Number)?)\\s*:?\\s*(\\d{10,18})");
        Matcher acctMatcher = acctPattern.matcher(text);
        if (acctMatcher.find()) {
            fields.put("accountNumber", acctMatcher.group(1));
        }

        // IFSC
        Matcher ifscMatcher = IFSC_PATTERN.matcher(text);
        if (ifscMatcher.find()) {
            fields.put("ifscCode", ifscMatcher.group());
        }
    }

    private void extractGenericFields(String text, Map<String, String> fields) {
        // Try PAN and Aadhaar patterns on any document
        Matcher panMatcher = PAN_PATTERN.matcher(text);
        if (panMatcher.find()) {
            fields.put("panNumber", panMatcher.group());
        }

        Matcher aadhaarMatcher = AADHAAR_PATTERN.matcher(text);
        if (aadhaarMatcher.find()) {
            String aadhaar = aadhaarMatcher.group().replaceAll("\\s", "");
            fields.put("aadhaarNumber", aadhaar);
        }

        extractNameField(text, fields);
    }

    private void extractNameField(String text, Map<String, String> fields) {
        Matcher nameMatcher = NAME_PATTERN.matcher(text);
        if (nameMatcher.find()) {
            fields.put("name", nameMatcher.group(1).trim());
        }
    }

    private double calculateConfidence(Map<String, String> fields, String documentType) {
        if (fields.isEmpty()) return 0.0;

        int expectedFields = switch (documentType) {
            case "PAN_CARD" -> 2;        // panNumber + name
            case "AADHAAR_CARD" -> 2;    // aadhaarNumber + name
            case "SALARY_SLIP" -> 4;     // name + gross + net + employer
            case "BANK_STATEMENT" -> 3;  // name + accountNumber + ifsc
            default -> 1;
        };

        return Math.min(1.0, (double) fields.size() / expectedFields);
    }
}
