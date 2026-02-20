package com.loanflow.loan.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable BPMN engine configuration.
 * Flowable tables (ACT_*) are created in the public schema,
 * separate from our application schema managed by Flyway.
 */
@Configuration
public class FlowableConfig {

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> engineConfigurer() {
        return engineConfiguration -> {
            // Disable Flowable's built-in identity service — we use Keycloak
            engineConfiguration.setDisableIdmEngine(true);
        };
    }
}
