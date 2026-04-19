package com.pix.engine.infrastructure.entrypoint.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pix.engine.application.usecase.ProcessPaymentResult;
import com.pix.engine.application.usecase.ProcessPaymentUseCase;
import com.pix.engine.domain.exception.InsufficientFundsException;
import com.pix.engine.domain.exception.InvalidPixKeyException;
import com.pix.engine.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PaymentController.class, PaymentControllerAdvice.class})
class PaymentControllerTest {

    private static final String URL = "/api/v1/payments";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ProcessPaymentUseCase useCase;

    private Map<String, Object> validBody() {
        return Map.of(
                "senderAccountId", UUID.randomUUID().toString(),
                "receiverAccountId", UUID.randomUUID().toString(),
                "amount", "50.00"
        );
    }

    @Test
    void givenNewPayment_whenProcessed_thenReturns201WithCreatedTrue() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        ProcessPaymentResult result = new ProcessPaymentResult(
                UUID.randomUUID(), idempotencyKey, "COMPLETED", true);

        when(useCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(true))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void givenIdempotentRetry_whenProcessed_thenReturns200WithCreatedFalse() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        ProcessPaymentResult result = new ProcessPaymentResult(
                UUID.randomUUID(), idempotencyKey, "COMPLETED", false);

        when(useCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(false));
    }

    @Test
    void givenInsufficientFunds_whenProcessed_thenReturns422() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(useCase.execute(any())).thenThrow(
                new InsufficientFundsException(accountId,
                        new Money(new BigDecimal("10.00"), "BRL"),
                        new Money(new BigDecimal("200.00"), "BRL")));

        mockMvc.perform(post(URL)
                        .header("X-Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Insufficient Funds"));
    }

    @Test
    void givenAccountNotFound_whenProcessed_thenReturns404() throws Exception {
        when(useCase.execute(any())).thenThrow(
                new IllegalArgumentException("Account not found: " + UUID.randomUUID()));

        mockMvc.perform(post(URL)
                        .header("X-Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void givenInvalidPixKey_whenProcessed_thenReturns404() throws Exception {
        when(useCase.execute(any())).thenThrow(new InvalidPixKeyException("bad@key.com"));

        mockMvc.perform(post(URL)
                        .header("X-Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Pix Key Not Found"));
    }

    @Test
    void givenMissingIdempotencyKeyHeader_whenRequested_thenReturns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing Required Header"));
    }

    @Test
    void givenNullSenderAccountId_whenRequested_thenReturns400WithFieldError() throws Exception {
        Map<String, Object> body = Map.of(
                "receiverAccountId", UUID.randomUUID().toString(),
                "amount", "50.00"
        );

        mockMvc.perform(post(URL)
                        .header("X-Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.errors.senderAccountId").exists());
    }

    @Test
    void givenNegativeAmount_whenRequested_thenReturns400WithFieldError() throws Exception {
        Map<String, Object> body = Map.of(
                "senderAccountId", UUID.randomUUID().toString(),
                "receiverAccountId", UUID.randomUUID().toString(),
                "amount", "-10.00"
        );

        mockMvc.perform(post(URL)
                        .header("X-Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }
}
