package com.loanflow.document.service.impl;

import com.loanflow.document.service.VirusScanResult;
import com.loanflow.document.service.VirusScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * No-op virus scan for dev/test environments where ClamAV is not available.
 * Always returns CLEAN.
 */
@Component
@Profile({"dev", "test", "default"})
@Slf4j
public class NoOpVirusScanService implements VirusScanService {

    @Override
    public VirusScanResult scan(MultipartFile file) {
        log.debug("NoOp virus scan: Skipping scan for '{}' (dev/test mode)",
                file.getOriginalFilename());
        return VirusScanResult.clean();
    }
}
