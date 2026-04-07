package com.example.hotelreservation.controller;

import com.example.hotelreservation.event.listener.BankTransferPaymentListener;
import com.example.hotelreservation.openapi.model.*;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
        partitions = 1,
        topics = {BankTransferPaymentListener.TOPIC},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class ConfirmReservationControllerIT {

    private static final String ENDPOINT = "/confirm-reservation";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();
    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {

        // Point the connector at the embedded WireMock server
        registry.add("credit-card-payment-api.base-url",
                () -> wireMock.baseUrl() + "/host/credit-card-payment-api");
    }

    @Test
    void shouldReturn200WithConfirmed_whenCashPayment() {

        ConfirmReservationRequest request = buildRequest(PaymentMode.CASH, "REF001");

        ResponseEntity<ConfirmReservationResponse> response =
                restTemplate.postForEntity(ENDPOINT, request, ConfirmReservationResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Status.CONFIRMED, response.getBody().getStatus());
        assertNotNull(response.getBody().getReservationId());
    }

    @Test
    void shouldReturn200WithPendingPayment_whenBankTransferPayment() {

        ConfirmReservationRequest request = buildRequest(PaymentMode.BANK_TRANSFER, "REF002");

        ResponseEntity<ConfirmReservationResponse> response =
                restTemplate.postForEntity(ENDPOINT, request, ConfirmReservationResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Status.PENDING_PAYMENT, response.getBody().getStatus());
        assertNotNull(response.getBody().getReservationId());
    }

    @Test
    void shouldReturn200WithConfirmed_whenCreditCardPaymentConfirmed() {

        wireMock.stubFor(post(urlEqualTo("/host/credit-card-payment-api/payment-status"))
                .willReturn(okJson("{\"status\":\"CONFIRMED\"}")));

        ConfirmReservationRequest request = buildRequest(PaymentMode.CREDIT_CARD, "CC-REF-001");

        ResponseEntity<ConfirmReservationResponse> response =
                restTemplate.postForEntity(ENDPOINT, request, ConfirmReservationResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Status.CONFIRMED, response.getBody().getStatus());
    }

    @Test
    void shouldReturn500_whenCreditCardPaymentRejected() {

        wireMock.stubFor(post(urlEqualTo("/host/credit-card-payment-api/payment-status"))
                .willReturn(okJson("{\"status\":\"REJECTED\"}")));

        ConfirmReservationRequest request = buildRequest(PaymentMode.CREDIT_CARD, "CC-REF-002");

        ResponseEntity<Void> response =
                restTemplate.postForEntity(ENDPOINT, request, Void.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturn400_whenReservationExceeds30Days() {

        ConfirmReservationRequest request = buildRequest(PaymentMode.CASH, "REF003");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(31));

        ResponseEntity<Void> response =
                restTemplate.postForEntity(ENDPOINT, request, Void.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private ConfirmReservationRequest buildRequest(PaymentMode paymentMode, String paymentReference) {

        ConfirmReservationRequest request = new ConfirmReservationRequest();
        request.setCustomerName("John Doe");
        request.setRoomNumber(101);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setRoomSegment(RoomSegment.SMALL);
        request.setPaymentMode(paymentMode);
        request.setPaymentReference(paymentReference);
        return request;
    }

}
