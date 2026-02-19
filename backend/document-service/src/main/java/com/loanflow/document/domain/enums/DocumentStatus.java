package com.loanflow.document.domain.enums;

/**
 * Status of document in its lifecycle
 */
public enum DocumentStatus {
    PENDING("Pending Upload"),
    UPLOADED("Uploaded"),
    UPLOAD_FAILED("Upload Failed"),
    VERIFIED("Verified"),
    REJECTED("Rejected"),
    EXPIRED("Expired"),
    DELETED("Deleted"),
    ARCHIVED("Archived");

    private final String displayName;

    DocumentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean requiresReupload() {
        return this == REJECTED || this == UPLOAD_FAILED || this == EXPIRED;
    }

    public boolean isTerminal() {
        return this == DELETED || this == ARCHIVED;
    }

    public boolean canBeVerified() {
        return this == UPLOADED || this == PENDING;
    }
}
