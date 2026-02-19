package com.loanflow.document.domain.enums;

/**
 * Types of documents that can be uploaded for loan applications
 */
public enum DocumentType {
    // KYC Documents
    PAN_CARD(DocumentCategory.KYC, false),
    AADHAAR_CARD(DocumentCategory.KYC, false),
    PASSPORT(DocumentCategory.KYC, true),
    VOTER_ID(DocumentCategory.KYC, false),
    DRIVING_LICENSE(DocumentCategory.KYC, true),

    // Income Documents
    SALARY_SLIP(DocumentCategory.INCOME, true),
    FORM_16(DocumentCategory.INCOME, true),
    ITR(DocumentCategory.INCOME, true),
    EMPLOYMENT_LETTER(DocumentCategory.INCOME, true),

    // Financial Documents
    BANK_STATEMENT(DocumentCategory.FINANCIAL, true),
    EXISTING_LOAN_STATEMENT(DocumentCategory.FINANCIAL, true),
    CREDIT_CARD_STATEMENT(DocumentCategory.FINANCIAL, true),

    // Property Documents (for Home/LAP loans)
    PROPERTY_DEED(DocumentCategory.PROPERTY, false),
    SALE_AGREEMENT(DocumentCategory.PROPERTY, false),
    PROPERTY_TAX_RECEIPT(DocumentCategory.PROPERTY, true),
    ENCUMBRANCE_CERTIFICATE(DocumentCategory.PROPERTY, true),
    APPROVED_PLAN(DocumentCategory.PROPERTY, false),
    NOC(DocumentCategory.PROPERTY, false),

    // Vehicle Documents
    VEHICLE_RC(DocumentCategory.VEHICLE, false),
    VEHICLE_INSURANCE(DocumentCategory.VEHICLE, true),
    QUOTATION(DocumentCategory.VEHICLE, true),

    // Business Documents
    GST_REGISTRATION(DocumentCategory.BUSINESS, false),
    BUSINESS_LICENSE(DocumentCategory.BUSINESS, true),
    PARTNERSHIP_DEED(DocumentCategory.BUSINESS, false),
    MOA_AOA(DocumentCategory.BUSINESS, false),
    BALANCE_SHEET(DocumentCategory.BUSINESS, true),
    PROFIT_LOSS(DocumentCategory.BUSINESS, true),

    // Other
    PHOTOGRAPH(DocumentCategory.OTHER, false),
    SIGNATURE(DocumentCategory.OTHER, false),
    CANCELLED_CHEQUE(DocumentCategory.OTHER, false),
    OTHER(DocumentCategory.OTHER, false);

    private final DocumentCategory category;
    private final boolean hasExpiry;

    DocumentType(DocumentCategory category, boolean hasExpiry) {
        this.category = category;
        this.hasExpiry = hasExpiry;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public boolean hasExpiry() {
        return hasExpiry;
    }

    /**
     * Get default expiry period in months
     */
    public int getDefaultExpiryMonths() {
        return switch (this) {
            case SALARY_SLIP -> 3;
            case BANK_STATEMENT, CREDIT_CARD_STATEMENT -> 6;
            case FORM_16, ITR, BALANCE_SHEET, PROFIT_LOSS -> 12;
            case PASSPORT, DRIVING_LICENSE, VEHICLE_INSURANCE, BUSINESS_LICENSE -> 60;
            case PROPERTY_TAX_RECEIPT, ENCUMBRANCE_CERTIFICATE -> 12;
            case EMPLOYMENT_LETTER, QUOTATION -> 3;
            case EXISTING_LOAN_STATEMENT -> 3;
            default -> 0; // No expiry
        };
    }
}
