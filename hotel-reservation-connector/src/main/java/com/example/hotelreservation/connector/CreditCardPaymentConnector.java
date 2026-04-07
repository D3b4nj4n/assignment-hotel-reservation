package com.example.hotelreservation.connector;

import com.example.creditcardpayment.model.PaymentStatusResponse;
import com.example.creditcardpayment.model.PaymentStatusRetrievalRequest;
import com.example.hotelreservation.exception.CreditCardPaymentConnectorException;
import com.example.hotelreservation.exception.ExceptionType;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

@Slf4j
@Component
public class CreditCardPaymentConnector {

    public static final String TRACE_ID_MDC_KEY = "traceId";

    private static final int TIMEOUT_MS = 5_000;
    private static final int MAX_ATTEMPTS = 5;
    private static final long INITIAL_BACKOFF_MS = 500L;
    private static final double BACKOFF_MULTIPLIER = 3;
    private static final long MAX_BACKOFF_MS = 8_000L;

    private final RestClient restClient;
    private final RetryTemplate retryTemplate;


    public CreditCardPaymentConnector(RestClient.Builder restClientBuilder, @Value("${credit-card-payment-api.base-url}") String baseUrl) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MS);
        requestFactory.setReadTimeout(TIMEOUT_MS);

        this.restClient = restClientBuilder.baseUrl(baseUrl).build();

        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(MAX_ATTEMPTS)
                .exponentialBackoff(INITIAL_BACKOFF_MS, BACKOFF_MULTIPLIER, MAX_BACKOFF_MS)
                .retryOn(ResourceAccessException.class)
                .withListener(new RetryListener() {
                    @Override
                    public <T, E extends Throwable> void onError(
                            RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                        log.warn("credit-card-payment-api call failed (attempt:{}/{}): {}",
                                context.getRetryCount(), MAX_ATTEMPTS, throwable.getMessage());
                    }
                })
                .build();
    }

    private static boolean isTimeout(ResourceAccessException ex) {

        Throwable cause = ex.getCause();
        return cause instanceof SocketTimeoutException
                || (null != cause && null != cause.getMessage()
                && cause.getMessage().toLowerCase().contains("timeout"));
    }

    public PaymentStatusResponse getPaymentStatus(String paymentReference) {

        log.info("calling credit-card-payment-api for paymentReference {}", paymentReference);
        String traceId = MDC.get(TRACE_ID_MDC_KEY);

        PaymentStatusRetrievalRequest request = new PaymentStatusRetrievalRequest();
        request.setPaymentReference(paymentReference);

        try {
            return retryTemplate.execute(ctx -> doGetPaymentStatus(request, traceId));
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                log.error("Timeout calling credit-card-payment-api for paymentReference: {} after {} attempts", paymentReference, MAX_ATTEMPTS);
                throw new CreditCardPaymentConnectorException(
                        String.format(
                                "Request to credit-card-payment-api timed out after %d ms for paymentReference: %s)",
                                TIMEOUT_MS, paymentReference),
                        ExceptionType.GATEWAY_TIMEOUT, ex);
            }
            log.error("Retries exhausted calling credit-crd-payment-api for paymentReference: {}", paymentReference, ex);
            throw new CreditCardPaymentConnectorException(
                    String.format(
                            "credit-card-payment-api is unavailable after %d attempts for paymentReference : %s",
                            MAX_ATTEMPTS, paymentReference),
                    ExceptionType.SERVICE_UNAVAILABLE, ex);
        }

    }

    private PaymentStatusResponse doGetPaymentStatus(PaymentStatusRetrievalRequest request, String traceId) {

        return restClient.post()
                .uri("/payment-status")
                .header("Trace-Id", traceId)
                .body(request)
                .retrieve()
                .body(PaymentStatusResponse.class);
    }

}
