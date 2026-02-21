package com.loanflow.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(exclude = {
        org.flowable.spring.boot.FlowableSecurityAutoConfiguration.class
})
@EnableJpaAuditing
@ComponentScan(basePackages = {
        "com.loanflow.loan",
        "com.loanflow.security",
        "com.loanflow.util"
})
public class LoanServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanServiceApplication.class, args);
    }
}
