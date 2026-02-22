package com.loanflow.document.service.impl;

import com.loanflow.document.service.VirusScanResult;
import com.loanflow.document.service.VirusScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * ClamAV-based virus scanning implementation.
 * Connects to clamd TCP socket and uses INSTREAM command.
 * Active in uat and prod profiles where ClamAV Docker container is running.
 */
@Component
@Profile({"uat", "prod"})
@Slf4j
public class ClamAvVirusScanService implements VirusScanService {

    private final ClamavClient clamavClient;

    public ClamAvVirusScanService(
            @Value("${clamav.host:localhost}") String host,
            @Value("${clamav.port:3310}") int port) {
        this.clamavClient = new ClamavClient(host, port);
        log.info("ClamAV virus scanner initialized: {}:{}", host, port);
    }

    @Override
    public VirusScanResult scan(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        log.info("Virus scan: Scanning file '{}' ({} bytes)", fileName, file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            ScanResult result = clamavClient.scan(inputStream);

            if (result instanceof ScanResult.OK) {
                log.info("Virus scan: File '{}' is CLEAN", fileName);
                return VirusScanResult.clean();
            } else if (result instanceof ScanResult.VirusFound virusFound) {
                Map<String, Collection<String>> viruses = virusFound.getFoundViruses();
                String virusNames = viruses.values().stream()
                        .flatMap(Collection::stream)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Unknown");
                log.warn("Virus scan: File '{}' is INFECTED — {}", fileName, virusNames);
                return VirusScanResult.infected(virusNames);
            } else {
                log.warn("Virus scan: Unexpected result type for file '{}'", fileName);
                return VirusScanResult.error("Unexpected scan result");
            }
        } catch (Exception e) {
            log.error("Virus scan: Failed to scan file '{}': {}", fileName, e.getMessage());
            return VirusScanResult.error("Virus scan failed: " + e.getMessage());
        }
    }
}
