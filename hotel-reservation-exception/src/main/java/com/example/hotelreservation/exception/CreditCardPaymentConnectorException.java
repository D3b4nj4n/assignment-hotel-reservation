package com.example.hotelreservation.exception;

public class CreditCardPaymentConnectorException extends ReservationException {

    public CreditCardPaymentConnectorException(String message, ExceptionType type) {
        super(message, type);
    }

    public CreditCardPaymentConnectorException(String message, ExceptionType type, Throwable cause) {
        super(message, type);
        initCause(cause);
    }

}
