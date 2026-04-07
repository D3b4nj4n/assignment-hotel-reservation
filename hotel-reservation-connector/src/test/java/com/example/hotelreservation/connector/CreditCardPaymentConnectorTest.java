package com.example.hotelreservation.connector;

import com.example.creditcardpayment.model.PaymentStatusResponse;
import com.example.hotelreservation.exception.CreditCardPaymentConnectorException;
import com.example.hotelreservation.exception.ExceptionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;


class CreditCardPaymentConnectorTest {

    private static final String BASE_URL = "http://localhost:8099";
    private static final String PAYMENT_REFERENCE = "PAY-REF-001";
    private static final String TRACE_ID = "trace-abc-123";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer mockServer;
    private CreditCardPaymentConnector connector;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        connector = new CreditCardPaymentConnector(restClientBuilder, BASE_URL);
        MDC.put(CreditCardPaymentConnector.TRACE_ID_MDC_KEY, TRACE_ID);
    }

    @Test
    void getPaymentStatus_returnsConfirmedResponse_whenApiReturnsConfirmed() throws Exception {
        PaymentStatusResponse expectedResponse = new PaymentStatusResponse();
        expectedResponse.setStatus(PaymentStatusResponse.StatusEnum.CONFIRMED);
        expectedResponse.setLastUpdateDate("2026-04-07");

        mockServer.expect(requestTo(BASE_URL + "/payment-status"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Trace-Id", TRACE_ID))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

        PaymentStatusResponse actual = connector.getPaymentStatus(PAYMENT_REFERENCE);

        assertThat(actual.getStatus()).isEqualTo(PaymentStatusResponse.StatusEnum.CONFIRMED);
        assertThat(actual.getLastUpdateDate()).isEqualTo("2026-04-07");
        mockServer.verify();
    }

    @Test
    void getPaymentStatus_returnsRejectedResponse_whenApiReturnsRejected() throws Exception {
        PaymentStatusResponse expectedResponse = new PaymentStatusResponse();
        expectedResponse.setStatus(PaymentStatusResponse.StatusEnum.REJECTED);

        mockServer.expect(requestTo(BASE_URL + "/payment-status"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

        PaymentStatusResponse actual = connector.getPaymentStatus(PAYMENT_REFERENCE);

        assertThat(actual.getStatus()).isEqualTo(PaymentStatusResponse.StatusEnum.REJECTED);
        mockServer.verify();
    }

    @Test
    void getPaymentStatus_sendsPaymentReferenceInRequestBodyAndTraceIdInHeader() throws Exception {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setStatus(PaymentStatusResponse.StatusEnum.CONFIRMED);

        mockServer.expect(requestTo(BASE_URL + "/payment-status"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.paymentReference").value(PAYMENT_REFERENCE))
                .andExpect(header("Trace-Id", TRACE_ID))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

        connector.getPaymentStatus(PAYMENT_REFERENCE);

        mockServer.verify();
    }

    @Test
    void getPaymentStatus_throwsCreditCardPaymentConnectorException_withGatewayTimeout_whenAllRetriesTimeOut() {
        // Respond with a connection-reset / IOException on every attempt
        mockServer.expect(times(5), requestTo(BASE_URL + "/payment-status"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> connector.getPaymentStatus(PAYMENT_REFERENCE))
                .isInstanceOf(CreditCardPaymentConnectorException.class)
                .satisfies(ex -> assertThat(((CreditCardPaymentConnectorException) ex).getType())
                        .isEqualTo(ExceptionType.GATEWAY_TIMEOUT));
    }

    @Test
    void getPaymentStatus_throwsCreditCardPaymentConnectorException_withServiceUnavailable_whenAllRetriesFailWithConnectionRefused() {
        mockServer.expect(times(5), requestTo(BASE_URL + "/payment-status"))
                .andRespond(withException(new java.net.ConnectException("Connection refused")));

        assertThatThrownBy(() -> connector.getPaymentStatus(PAYMENT_REFERENCE))
                .isInstanceOf(CreditCardPaymentConnectorException.class)
                .satisfies(ex -> assertThat(((CreditCardPaymentConnectorException) ex).getType())
                        .isEqualTo(ExceptionType.SERVICE_UNAVAILABLE));
    }


    @Test
    void getPaymentStatus_retriesAndSucceeds_afterTransientFailures() throws Exception {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setStatus(PaymentStatusResponse.StatusEnum.CONFIRMED);

        // First two attempts fail, third succeeds
        mockServer.expect(times(2), requestTo(BASE_URL + "/payment-status"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));
        mockServer.expect(requestTo(BASE_URL + "/payment-status"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

        PaymentStatusResponse actual = connector.getPaymentStatus(PAYMENT_REFERENCE);

        assertThat(actual.getStatus()).isEqualTo(PaymentStatusResponse.StatusEnum.CONFIRMED);
        mockServer.verify();
    }

}
