package ru.finuniversity.advance.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.finuniversity.advance.payment.client.MockSbpClient;
import ru.finuniversity.advance.payment.dto.SbpResponse;
import ru.finuniversity.advance.payment.entity.Payment;
import ru.finuniversity.advance.payment.exception.SbpTimeoutException;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SbpPaymentService {

    private final MockSbpClient sbpClient;

    public void initiatePayment(Payment payment) {
        String sbpRequestId = UUID.randomUUID().toString();
        payment.setSbpRequestId(sbpRequestId);
        payment.setStatus("PROCESSING");

        try {
            SbpResponse response = sbpClient.transfer(
                    payment.getRecipientPhone(),
                    payment.getAmount(),
                    sbpRequestId
            );
            payment.setStatus("COMPLETED");
            payment.setSbpResponseCode(response.responseCode());
            payment.setCompletedAt(Instant.now());
            log.info("SBP payment COMPLETED: advanceId={} requestId={}", payment.getAdvanceId(), sbpRequestId);
        } catch (SbpTimeoutException ex) {
            payment.setStatus("FAILED");
            payment.setErrorMessage(ex.getMessage());
            log.warn("SBP payment FAILED: advanceId={} error={}", payment.getAdvanceId(), ex.getMessage());
        }
    }
}
