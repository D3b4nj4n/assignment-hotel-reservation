package com.example.hotelreservation.event.listener;

import com.example.hotelreservation.event.model.BankTransferPaymentEvent;
import com.example.hotelreservation.event.service.BankTransferPaymentService;
import com.example.hotelreservation.exception.ReservationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka listener that consumes bank transfer payment events from the {@value #TOPIC} topic and
 * delegates processing to {@link BankTransferPaymentService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankTransferPaymentListener {

    // Kafka topic from which bank transfer payment events are consumed
    public static final String TOPIC = "bank-transfer-payment-update";

    // Consumer group ID used by this listener
    public static final String GROUP_ID = "hotel-reservation-group";

    private final BankTransferPaymentService bankTransferPaymentService;

    /**
     * Handles an incoming {@link BankTransferPaymentEvent}.
     *
     * <p>Delegates to {@link BankTransferPaymentService#processPaymentUpdate(BankTransferPaymentEvent)}.
     * Business errors ({@link ReservationException}) are logged and swallowed so the offset is
     * committed and the event is not retried. Unexpected errors are re-thrown for the container to handle.
     *
     * @param event     the deserialized bank transfer payment event
     * @param partition the Kafka partition the message was received from
     * @param offset    the offset of the message within its partition
     */
    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP_ID,
            containerFactory = "bankTransferKafkaListenerContainerFactory"
    )
    public void onPaymentUpdate(
            @Payload BankTransferPaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {

        log.info("Received bank-transfer-payment-update event from partition: {}, offset: {}, paymentId: {}",
                partition, offset, event.getPaymentId());

        try {
            bankTransferPaymentService.processPaymentUpdate(event);
        } catch (ReservationException e) {
            //Currently sweallowing the event so that Kafka does not keep retrying
            //Retry configuration can be implemented here
            log.error("Business error while processing payment event for paymentId= '{}': [{}] {}",
                    event.getPaymentId(), e.getType(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while processing payment event for paymentId= '{}': {}",
                    event.getPaymentId(), e.getMessage(), e);
            throw e;
        }
    }

}
