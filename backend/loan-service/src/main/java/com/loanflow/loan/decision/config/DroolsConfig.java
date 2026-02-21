package com.loanflow.loan.decision.config;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Drools configuration for the Decision Engine.
 * Compiles DRL rule files at startup and provides a KieContainer bean.
 * KieContainer is thread-safe; KieSessions are created per-request.
 */
@Configuration
@Slf4j
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer() {
        log.info("Initializing Drools KieContainer with DRL rule files...");

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        // Load DRL files from classpath
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/eligibility-rules.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/pricing-rules.drl"));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll();

        // Fail fast on DRL compilation errors
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            String errors = kieBuilder.getResults().getMessages().toString();
            log.error("Drools DRL compilation failed: {}", errors);
            throw new IllegalStateException("Drools DRL compilation errors: " + errors);
        }

        if (kieBuilder.getResults().hasMessages(Message.Level.WARNING)) {
            log.warn("Drools DRL compilation warnings: {}",
                    kieBuilder.getResults().getMessages());
        }

        KieContainer container = kieServices.newKieContainer(
                kieServices.getRepository().getDefaultReleaseId());

        log.info("Drools KieContainer initialized successfully");
        return container;
    }
}
