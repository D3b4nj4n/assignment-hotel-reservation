package com.example.hotelreservation.exceptionhandler;

import com.example.hotelreservation.exception.ExceptionType;
import com.example.hotelreservation.exception.ReservationException;
import com.example.hotelreservation.filter.TraceIdFilter;
import com.example.hotelreservation.openapi.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.http.HttpStatus.*;

/**
 * Exception handler class to handle exceptions thrown by different classes
 */
@Slf4j
@ControllerAdvice
public class ReservationExceptionHandler {

    /**
     * Handle the parent {@link ReservationException} thrown by other classes
     *
     * @param exception the {@link ReservationException} exception thrown
     * @return {@link ResponseEntity<ErrorResponse>} the response entity containing the errors
     */
    @ExceptionHandler(ReservationException.class)
    public ResponseEntity<ErrorResponse> handleReservationException(ReservationException exception) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(buildErrorResponse(exception), headers, getHttpStatus(exception));
    }

    /**
     * Handle the {@link HttpMessageNotReadableException} thrown during validation failure
     *
     * @param exception the {@link HttpMessageNotReadableException} exception thrown
     * @return {@link ResponseEntity<ErrorResponse>} the response entity containing the errors
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(buildErrorResponse(exception), headers, BAD_REQUEST);
    }

    /**
     * Handle the {@link ConstraintViolationException} thrown during validation failure
     *
     * @param exception the {@link ConstraintViolationException} exception thrown
     * @return {@link ResponseEntity<ErrorResponse>} the response entity containing the errors
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(buildErrorResponse(exception), headers, BAD_REQUEST);
    }

    /**
     * Handle the {@link MethodArgumentNotValidException} thrown during validation failure
     *
     * @param exception the {@link MethodArgumentNotValidException} exception thrown
     * @return {@link ResponseEntity<ErrorResponse>} the response entity containing the errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(buildErrorResponse(exception), headers, BAD_REQUEST);
    }

    /**
     * Handle the {@link DataIntegrityViolationException} thrown during database operation
     *
     * @param exception the {@link DataIntegrityViolationException} exception thrown
     * @return {@link ResponseEntity<ErrorResponse>} the response entity containing the errors
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(buildErrorResponse(exception), headers, INTERNAL_SERVER_ERROR);
    }

    /**
     * Generates the HttpStatus for the response from the overridingType of the {@link ReservationException}
     *
     * @param reservationException the exception thrown
     * @return {@link HttpStatus} http status
     */
    private HttpStatus getHttpStatus(ReservationException reservationException) {

        return toHttpStatus(reservationException.getType());
    }

    /**
     * Generates the HttpStatus from the overriding exception type enum
     *
     * @param exceptionType rhe overriding type of exception
     * @return {@link HttpStatus} http status
     */
    private HttpStatus toHttpStatus(ExceptionType exceptionType) {

        return valueOf(exceptionType.getCode());
    }

    /**
     * Builds the response to be provided with error details
     *
     * @param exception the exception thrown
     * @return {@link ErrorResponse} the response object
     */
    private ErrorResponse buildErrorResponse(Exception exception) {

        return new ErrorResponse()
                .error(exception.getLocalizedMessage())
                .traceId(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
    }

}
