package ru.leychenkov.triage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.leychenkov.triage.event.RawErrorEvent;
import ru.leychenkov.triage.service.ErrorProducerService;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Profile("dev")
@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
public class TestErrorGeneratorController {

    private final ErrorProducerService errorProducerService;

    @PostMapping("/simulate-storm")
    public Map<String, Object> simulateErrorStorm(@RequestParam(defaultValue = "10") int count) {
        for (int i = 0; i < count; i++) {
            RawErrorEvent event = RawErrorEvent.builder()
                    .serviceName("payment-service")
                    .environment("prod")
                    .exceptionClass("java.lang.NullPointerException")
                    .errorMessage("Cannot invoke \"Wallet.getBalance()\" because \"wallet\" is null for user " + (1000 + i))
                    .stackTrace("""
                            java.lang.NullPointerException: Cannot invoke "Wallet.getBalance()" because "wallet" is null
                                at ru.company.payment.service.BillingService.charge(BillingService.java:84)
                                at ru.company.payment.controller.PaymentController.pay(PaymentController.java:42)
                            """)
                    .traceId(UUID.randomUUID().toString())
                    .occurredAt(Instant.now())
                    .build();

            errorProducerService.send(event);
        }

        return Map.of(
                "status", "sent",
                "eventsPublished", count
        );
    }
}
