package com.loanflow.document.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for virus scanning documents before storage.
 * Implementations:
 * - ClamAvVirusScanService (@Profile uat/prod) — real ClamAV scan via clamd TCP
 * - NoOpVirusScanService (@Profile dev/test/default) — always passes (no ClamAV needed)
 */
public interface VirusScanService {

    /**
     * Scan a file for viruses.
     *
     * @param file The uploaded file to scan
     * @return VirusScanResult indicating clean, infected, or error
     */
    VirusScanResult scan(MultipartFile file);
}
