package com.pix.engine.infrastructure.entrypoint.rest;

import com.pix.engine.application.usecase.ProcessPaymentCommand;
import com.pix.engine.application.usecase.ProcessPaymentResult;
import com.pix.engine.application.usecase.ProcessPaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Payments", description = "Pix payment processing endpoint")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final ProcessPaymentUseCase useCase;

    public PaymentController(ProcessPaymentUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(
        summary = "Process a Pix payment",
        description = """
            Initiates a Pix payment between two accounts.
            The `X-Idempotency-Key` header (a client-generated UUID) guarantees
            exactly-once semantics: retrying the same request with the same key
            returns the original result without re-processing the payment.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Payment created and processed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ProcessPaymentResult.class)
            )
        ),
        @ApiResponse(
            responseCode = "200",
            description = "Idempotent retry — payment was already processed; original result returned",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ProcessPaymentResult.class)
            )
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Business rule violation (e.g., insufficient funds, inactive account)",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body or missing required fields",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Sender or receiver account not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected internal error",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<ProcessPaymentResult> processPayment(
            @Parameter(
                description = "Client-generated UUID used for idempotency. " +
                              "Re-sending the same key within 24 h returns the original response.",
                required = true,
                example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @RequestHeader("X-Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        ProcessPaymentCommand command = new ProcessPaymentCommand(
                idempotencyKey,
                request.senderAccountId(),
                request.receiverAccountId(),
                request.amount()
        );

        ProcessPaymentResult result = useCase.execute(command);

        HttpStatus responseStatus = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(responseStatus).body(result);
    }
}
