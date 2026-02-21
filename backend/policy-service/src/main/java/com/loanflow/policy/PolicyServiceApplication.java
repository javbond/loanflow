package com.loanflow.policy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Policy Service Application
 * Dynamic policy engine for loan eligibility, pricing, and credit limit rules
 */
@SpringBootApplication(scanBasePackages = {
        "com.loanflow.policy",
        "com.loanflow.security",
        "com.loanflow.util"
})
public class PolicyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolicyServiceApplication.class, args);
    }
}
