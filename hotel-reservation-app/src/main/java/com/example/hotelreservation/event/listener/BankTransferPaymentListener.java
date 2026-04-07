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

@Slf4j
@Component
@RequiredArgsConstructor
public class BankTransferPaymentListener {

    public static final String TOPIC = "bank-transfer-payment-update";

    public static final String GROUP_ID = "hotel-reservation-group";

    private final BankTransferPaymentService bankTransferPaymentService;

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
            //Dead Letter/  retry configuration can be implemented here
            log.error("Business error while processing payment event for paymentId= '{}': [{}] {}",
                    event.getPaymentId(), e.getType(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while processing payment event for paymentId= '{}': {}",
                    event.getPaymentId(), e.getMessage(), e);
            throw e;
        }
    }

}
