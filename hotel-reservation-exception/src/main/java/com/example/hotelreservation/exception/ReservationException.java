package com.example.hotelreservation.exception;

import java.util.Optional;

public class ReservationException extends RuntimeException {

    private ExceptionType overridingType;

    public ReservationException(String message) {
        super(message);
    }

    public ReservationException(String message, ExceptionType overridingType) {
        super(message);
        this.overridingType = overridingType;
    }

    public ExceptionType defaultType() {
        return ExceptionType.SERVER_ERROR;
    }

    public ExceptionType getType() {
        return Optional.ofNullable(overridingType).orElseGet(this::defaultType);
    }

}
