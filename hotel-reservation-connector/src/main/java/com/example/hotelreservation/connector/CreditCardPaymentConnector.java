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

/**
 * Implementation of the connector to consume credit-card-payment-api
 */
@Slf4j
@Component
public class CreditCardPaymentConnector {

    public static final String TRACE_ID_MDC_KEY = "traceId";

    private static final int TIMEOUT_MS = 5_000;
    private static final int MAX_ATTEMPTS = 5;
    private static final long INITIAL_BACKOFF_MS = 500L;
    private static final double BACKOFF_MULTIPLIER = 2;
    private static final long MAX_BACKOFF_MS = 10_000L;

    private final RestClient restClient;
    private final RetryTemplate retryTemplate;


    /**
     * Constructor that builds RetryTemplate which is used in conjunction with RestClient
     * The retryTemplate is used to implement retry mechanism for calling the api with attributes
     * <ul>
     *     <li>Default timeout of the api - 5s</li>
     *     <li>Maximum Retry attempts - 5</li>
     *     <li>Initial backoff - 500ms</li>
     *     <li>Backoff multiplier - 2</li>
     *     <li>Maximum backoff period - 10s</li>
     * </ul>
     * <p>
     * If no connectivity is established with the api within 5ms, then it is timed out.
     * In case of no response received, it will retry after 500ms, going gradually upwards by 2x for each next retry
     * Once all retries are exhausted, throw an exception
     *
     * @param restClientBuilder the RestClient.Builder object
     * @param baseUrl           the url where the api is hosted
     */
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

    /**
     * Checks if timeout is applicable due to SocketTimeoutException
     *
     * @param ex ResourceAccessException object reference which is validated for timeout
     * @return if timeout is applicable
     */
    private static boolean isTimeout(ResourceAccessException ex) {

        Throwable cause = ex.getCause();
        return cause instanceof SocketTimeoutException
                || (null != cause && null != cause.getMessage()
                && cause.getMessage().toLowerCase().contains("timeout"));
    }

    /**
     * Retrieve the payment status from the credit-card-payment-api based on payment reference
     *
     * @param paymentReference the reference to the payment
     * @return {@link PaymentStatusResponse} the response received from the api
     */
    public PaymentStatusResponse getPaymentStatus(String paymentReference) {

        log.info("calling credit-card-payment-api for paymentReference {}", paymentReference);
        String traceId = MDC.get(TRACE_ID_MDC_KEY);

        PaymentStatusRetrievalRequest request = new PaymentStatusRetrievalRequest();
        request.setPaymentReference(paymentReference);

        try {
            //call the api based on the retryTemplate configurations
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

    /**
     * Actual invocation of the credit-card-payment-api
     *
     * @param request the request object to be passed
     * @param traceId to trace the request end-to-end
     * @return {@link PaymentStatusResponse} response received from the api invocation
     */
    private PaymentStatusResponse doGetPaymentStatus(PaymentStatusRetrievalRequest request, String traceId) {

        return restClient.post()
                .uri("/payment-status")
                .header("Trace-Id", traceId)
                .body(request)
                .retrieve()
                .body(PaymentStatusResponse.class);
    }

}
