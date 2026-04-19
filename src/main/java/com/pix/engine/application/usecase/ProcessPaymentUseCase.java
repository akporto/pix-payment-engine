package com.pix.engine.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pix.engine.domain.model.Account;
import com.pix.engine.domain.model.Money;
import com.pix.engine.domain.model.OutboxEvent;
import com.pix.engine.domain.model.Payment;
import com.pix.engine.domain.exception.InsufficientFundsException;
import com.pix.engine.domain.exception.InvalidPixKeyException;
import com.pix.engine.domain.port.out.AccountRepository;
import com.pix.engine.domain.port.out.IdempotencyRepository;
import com.pix.engine.domain.port.out.OutboxRepository;
import com.pix.engine.domain.port.out.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ProcessPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentUseCase.class);

    private static final String CURRENCY = "BRL";
    private static final String EVENT_TYPE = "PAYMENT_COMPLETED";

    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private final TransactionTemplate transactionTemplate;

    public ProcessPaymentUseCase(AccountRepository accountRepository,
                                 PaymentRepository paymentRepository,
                                 OutboxRepository outboxRepository,
                                 IdempotencyRepository idempotencyRepository,
                                 ObjectMapper objectMapper,
                                 PlatformTransactionManager transactionManager,
                                 MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ProcessPaymentResult execute(ProcessPaymentCommand command) {
        try {

            Optional<String> cached = idempotencyRepository.get(command.transactionId());
            if (cached.isPresent()) {
                ProcessPaymentResult fromCache = deserialize(cached.get(), command.transactionId());
                if (fromCache != null) {
                    ProcessPaymentResult replay = new ProcessPaymentResult(
                            fromCache.paymentId(),
                            fromCache.transactionId(),
                            fromCache.status(),
                            false
                    );
                    meterRegistry.counter("pix.payment.processing", "outcome", "success").increment();
                    return replay;
                }
            }

            accountRepository.findById(command.senderAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + command.senderAccountId()));
            accountRepository.findById(command.receiverAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + command.receiverAccountId()));

            Money amount = new Money(command.amount(), CURRENCY);

            Optional<Payment> existingOutsideTx = paymentRepository.findByTransactionId(command.transactionId());
            if (existingOutsideTx.isPresent()) {
                ProcessPaymentResult idempotent = toResult(existingOutsideTx.get(), false);
                idempotencyRepository.save(command.transactionId(), serialize(idempotent));
                meterRegistry.counter("pix.payment.processing", "outcome", "success").increment();
                return idempotent;
            }

            ProcessPaymentResult result = transactionTemplate.execute(status -> {

                List<UUID> orderedIds = Stream.of(command.senderAccountId(), command.receiverAccountId())
                        .sorted()
                        .toList();

                Account first  = accountRepository.findByIdWithLock(orderedIds.get(0));
                Account second = accountRepository.findByIdWithLock(orderedIds.get(1));

                Optional<Payment> existingUnderLock = paymentRepository.findByTransactionId(command.transactionId());
                if (existingUnderLock.isPresent()) {
                    return toResult(existingUnderLock.get(), false);
                }

                Account sender   = first.getId().equals(command.senderAccountId()) ? first : second;
                Account receiver = first.getId().equals(command.receiverAccountId()) ? first : second;

                sender.debit(amount);
                receiver.credit(amount);
                accountRepository.save(sender);
                accountRepository.save(receiver);

                Payment payment = new Payment(
                        UUID.randomUUID(),
                        command.transactionId(),
                        command.senderAccountId(),
                        command.receiverAccountId(),
                        amount
                );
                payment.complete();
                paymentRepository.save(payment);

                OutboxEvent outboxEvent = new OutboxEvent(
                        UUID.randomUUID(),
                        payment.getId(),
                        EVENT_TYPE,
                        buildPayload(payment)
                );
                outboxRepository.save(outboxEvent);

                return toResult(payment, true);
            });

            idempotencyRepository.save(command.transactionId(), serialize(result));

            log.debug("Committed payment transactionId={} paymentId={}", result.transactionId(), result.paymentId());
            meterRegistry.counter("pix.payment.processing", "outcome", "success").increment();
            return result;

        } catch (InsufficientFundsException | InvalidPixKeyException ex) {
            meterRegistry.counter("pix.payment.processing", "outcome", "business_error").increment();
            throw ex;
        }
    }

    private ProcessPaymentResult deserialize(String json, UUID transactionId) {
        try {
            return objectMapper.readValue(json, ProcessPaymentResult.class);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize cached result for transactionId {}, falling through to DB. Cause: {}",
                    transactionId, ex.getMessage());
            return null;
        }
    }

    private String serialize(ProcessPaymentResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize result for Redis cache. Cause: {}", ex.getMessage());
            return null;
        }
    }

    private String buildPayload(Payment payment) {
        return String.format(
                "{\"paymentId\":\"%s\",\"transactionId\":\"%s\"," +
                "\"senderAccountId\":\"%s\",\"receiverAccountId\":\"%s\"," +
                "\"amount\":\"%s\",\"currency\":\"%s\",\"status\":\"%s\"}",
                payment.getId(),
                payment.getTransactionId(),
                payment.getSenderAccountId(),
                payment.getReceiverAccountId(),
                payment.getAmount().amount(),
                payment.getAmount().currency(),
                payment.getStatus()
        );
    }

    private ProcessPaymentResult toResult(Payment payment, boolean created) {
        return new ProcessPaymentResult(
                payment.getId(),
                payment.getTransactionId(),
                payment.getStatus().name(),
                created
        );
    }
}
