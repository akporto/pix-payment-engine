package com.pix.engine.infrastructure.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI pixPaymentEngineOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Pix Payment Engine API")
                .description("""
                    High-concurrency Pix payment simulator built on Clean Architecture.

                    **Key guarantees:**
                    - **Exactly-once semantics** via `X-Idempotency-Key` header (Redis fast path + DB fallback).
                    - **Pessimistic locking** with deadlock-safe ordered lock acquisition.
                    - **Transactional Outbox Pattern** ensuring atomic DB writes and Kafka event publishing.
                    - **Graceful degradation**: Redis unavailability never blocks payment processing.
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Pix Payment Engine")
                    .email("tech@pix-engine.internal"))
                .license(new License()
                    .name("MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development")
            ));
    }
}
