package com.example.hotelreservation.exceptionhandler;

import com.example.hotelreservation.exception.ExceptionType;
import com.example.hotelreservation.exception.ReservationException;
import com.example.hotelreservation.filter.TraceIdFilter;
import com.example.hotelreservation.openapi.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationExceptionHandlerTest {

    private static final String TRACE_ID = "test-trace-id";
    @InjectMocks
    private ReservationExceptionHandler exceptionHandler;
    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @BeforeEach
    void setUpMdc() {

        MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, TRACE_ID);
    }

    @AfterEach
    void clearMdc() {

        MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
    }

    @Test
    void handleReservationException_withOverridingType_returnsCorrectStatus() {

        ReservationException exception = new ReservationException("reservation not found", ExceptionType.NOT_FOUND);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleReservationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    }

    @Test
    void handleReservationException_withNoOverridingType_returnsServerError() {

        ReservationException exception = new ReservationException("an issue has occurred");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleReservationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @Test
    void handleReservationException_returnsJsonContentType() {

        ReservationException exception = new ReservationException("bad request provided", ExceptionType.BAD_REQUEST);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleReservationException(exception);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

    }

    @Test
    void handleReservationException_bodyContainsMessageAndTraceId() {

        ReservationException exception = new ReservationException("an issue has occurred", ExceptionType.CONFLICT);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleReservationException(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("an issue has occurred");
        assertThat(response.getBody().getTraceId()).isEqualTo(TRACE_ID);

    }

    @Test
    void handleHttpMessageNotReadableException_returnsBadRequest() {

        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "malformed JSON", new MockHttpInputMessage("{}".getBytes(StandardCharsets.UTF_8)));

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadableException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTraceId()).isEqualTo(TRACE_ID);

    }

    @Test
    void handleConstraintViolationException_returnsBadRequest() {

        ConstraintViolationException exception = new ConstraintViolationException("invalid field", Set.of());

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("invalid field");

    }

    @Test
    void handleMethodArgumentNotValidException_returnsBadRequest() {

        when(methodArgumentNotValidException.getLocalizedMessage()).thenReturn("argument not valid");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentNotValidException(methodArgumentNotValidException);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("argument not valid");

    }

    @Test
    void handleDataIntegrityViolationException_returnsInternalServerError() {

        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate key");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).contains("duplicate key");

    }

}