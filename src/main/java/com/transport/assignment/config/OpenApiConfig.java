package com.transport.assignment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transporter Assignment API")
                        .version("1.0.0")
                        .description(
                                "API for submitting transporter quotes " +
                                        "and calculating cost-optimized lane assignments."));
    }
}