package com.assessment.interest_calculator.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(DigioAAProperties.class)
@Slf4j
@RequiredArgsConstructor
public class DigioAAConfig {
    private final DigioAAProperties properties;

    @Bean
    public WebClient digioWebClient() {
        log.info("Initializing Digio WebClient with base URL: {}", properties.getBaseUrl());
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("accept", "application/json")
                .defaultHeader("content-type", "application/json")
                .defaultHeader("client_id", properties.getApiKey())
                .defaultHeader("client_secret", properties.getApiSecret())
                .build();
    }

}
