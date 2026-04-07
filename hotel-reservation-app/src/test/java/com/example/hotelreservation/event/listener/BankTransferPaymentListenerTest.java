package com.example.hotelreservation.event.listener;

import com.example.hotelreservation.event.model.BankTransferPaymentEvent;
import com.example.hotelreservation.event.service.BankTransferPaymentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.argThat;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {BankTransferPaymentListener.TOPIC},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers" //this tells the embedded broker to set spring.kafka.bootstrap-servers to its own address directly, scoped only to this test.
)
class BankTransferPaymentListenerTest {

    @Autowired
    private KafkaTemplate<String, BankTransferPaymentEvent> kafkaTemplate;

    @MockitoBean
    private BankTransferPaymentService bankTransferPaymentService;

    @Test
    void shouldProcessPaymentUpdateEvent() throws Exception {
        BankTransferPaymentEvent event = new BankTransferPaymentEvent();
        event.setPaymentId("pay-001");
        event.setDebtorAccountNumber("NL12BANK0123456789");
        event.setAmountReceived("150.00");
        event.setTransactionDescription("E2EREFERENCE res-123");

        kafkaTemplate.send(BankTransferPaymentListener.TOPIC, event);

        // Give the listener time to process
        Thread.sleep(1000);

        Mockito.verify(bankTransferPaymentService).processPaymentUpdate(
                argThat(e -> "pay-001".equals(e.getPaymentId())
                ));
    }

}