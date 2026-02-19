package com.loanflow.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Document Service Application
 * Handles document upload, storage, and verification for loan applications
 */
@SpringBootApplication(scanBasePackages = {
        "com.loanflow.document",
        "com.loanflow.security",
        "com.loanflow.util"
})
@EnableMongoAuditing
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
