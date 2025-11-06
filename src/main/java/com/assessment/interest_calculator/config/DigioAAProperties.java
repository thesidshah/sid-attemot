package com.assessment.interest_calculator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "digio.aa")
public class DigioAAProperties {
    private String baseUrl;
    private String apiKey;
    private String apiSecret;
    private String templateId;
    private String webhookBaseUrl;
    private String publicKeyPath;
    private String privateKeyPath;
}
