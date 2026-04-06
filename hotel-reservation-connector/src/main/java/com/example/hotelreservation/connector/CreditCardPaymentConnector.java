package com.example.hotelreservation.connector;

import com.example.creditcardpayment.model.PaymentStatusResponse;
import com.example.creditcardpayment.model.PaymentStatusRetrievalRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class CreditCardPaymentConnector {

    public static final String TRACE_ID_MDC_KEY = "traceId";
    private final RestClient restClient;
    String traceId = MDC.get(TRACE_ID_MDC_KEY);

    public CreditCardPaymentConnector(RestClient.Builder restClientBuilder, @Value("${credit-card-payment-api.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public PaymentStatusResponse getPaymentStatus(String paymentReference) {
        log.info("calling credit-card-payment-api for paymentReference {}", paymentReference);

        PaymentStatusRetrievalRequest paymentStatusRetrievalRequest = new PaymentStatusRetrievalRequest();
        paymentStatusRetrievalRequest.setPaymentReference(paymentReference);

        return restClient.post()
                .uri("/payment-status")
                .header("Trace-Id", traceId)
                .body(paymentStatusRetrievalRequest)
                .retrieve()
                .body(PaymentStatusResponse.class);
    }

}
