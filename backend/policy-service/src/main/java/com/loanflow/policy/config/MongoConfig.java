package com.loanflow.policy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB configuration for policy service.
 * Separated from main application class to allow @WebMvcTest slicing
 * without requiring MongoDB context beans.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
