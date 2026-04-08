package com.example.hotelreservation.controller;

import com.example.hotelreservation.converter.ConfirmReservationRequestConverter;
import com.example.hotelreservation.converter.ConfirmReservationResponseConverter;
import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.exception.ExceptionType;
import com.example.hotelreservation.exception.ReservationException;
import com.example.hotelreservation.openapi.ConfirmReservationApi;
import com.example.hotelreservation.openapi.model.ConfirmReservationRequest;
import com.example.hotelreservation.openapi.model.ConfirmReservationResponse;
import com.example.hotelreservation.service.ConfirmReservationService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Rest Controller that implements the Api endpoint
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ConfirmReservationController implements ConfirmReservationApi {

    private static final long REQUEST_TIMEOUT_SECONDS = 10;
    private final ConfirmReservationService confirmReservationService;
    private final ConfirmReservationRequestConverter requestConverter;
    private final ConfirmReservationResponseConverter responseConverter;

    @Override
    public ResponseEntity<ConfirmReservationResponse> confirmReservation(
            @Parameter(name = "Trace-Id", description = "Id to trace the request end-to-end", in = ParameterIn.HEADER) @RequestHeader(value = "Trace-Id", required = false) String traceId,
            @Parameter(name = "ConfirmReservationRequest", description = "", required = true) @Valid @RequestBody ConfirmReservationRequest confirmReservationRequest
    ) {

        Room roomEntity = requestConverter.convert(confirmReservationRequest);

        try {
            Room savedRoom = CompletableFuture
                    .supplyAsync(() -> confirmReservationService.confirmReservation(roomEntity))
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            ConfirmReservationResponse response = responseConverter.convert(savedRoom);
            return ResponseEntity.ok(response);
        } catch (TimeoutException e) {
            throw new ReservationException("Request timed out after " + REQUEST_TIMEOUT_SECONDS + " seconds", ExceptionType.GATEWAY_TIMEOUT);
        } catch (ExecutionException e) {
            throw new ReservationException(e.getLocalizedMessage(), ExceptionType.SERVER_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReservationException("Request was interrupted", ExceptionType.SERVER_ERROR);
        }
    }

}
