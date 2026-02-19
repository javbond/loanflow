package com.loanflow.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak configuration properties
 * PRD Compliant: Configures Keycloak OAuth2/OIDC endpoints
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    /**
     * Keycloak server URL (e.g., http://localhost:8180)
     */
    private String serverUrl = "http://localhost:8180";

    /**
     * Keycloak realm name
     */
    private String realm = "loanflow";

    /**
     * OAuth2 client ID for this application
     */
    private String clientId = "loanflow-web";

    /**
     * OAuth2 client secret (for confidential clients)
     */
    private String clientSecret;

    /**
     * Get the token endpoint URL
     */
    public String getTokenUri() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    /**
     * Get the logout endpoint URL
     */
    public String getLogoutUri() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    /**
     * Get the end session endpoint URL (for OIDC logout redirect)
     */
    public String getEndSessionUri() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    /**
     * Get the userinfo endpoint URL
     */
    public String getUserInfoUri() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";
    }

    /**
     * Get the JWKS URI for JWT validation
     */
    public String getJwksUri() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/certs";
    }

    /**
     * Get the issuer URI for JWT validation
     */
    public String getIssuerUri() {
        return serverUrl + "/realms/" + realm;
    }
}
