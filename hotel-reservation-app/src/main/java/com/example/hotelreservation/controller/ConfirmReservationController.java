package com.example.hotelreservation.controller;

import com.example.hotelreservation.converter.ConfirmReservationRequestConverter;
import com.example.hotelreservation.converter.ConfirmReservationResponseConverter;
import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.openapi.ConfirmReservationApi;
import com.example.hotelreservation.openapi.model.ConfirmReservationRequest;
import com.example.hotelreservation.openapi.model.ConfirmReservationResponse;
import com.example.hotelreservation.service.ConfirmReservationService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ConfirmReservationController implements ConfirmReservationApi {

    private final ConfirmReservationService confirmReservationService;

    private final ConfirmReservationRequestConverter requestConverter;

    private final ConfirmReservationResponseConverter responseConverter;

    @Override
    public ResponseEntity<ConfirmReservationResponse> confirmReservationPost(
            @Parameter(name = "ConfirmReservationRequest", description = "", required = true) @Valid @RequestBody ConfirmReservationRequest confirmReservationRequest
    ) {

        Room roomEntity = requestConverter.convert(confirmReservationRequest);
        Room room = confirmReservationService.confirmReservation(roomEntity);
        ConfirmReservationResponse response = responseConverter.convert(room);
        return ResponseEntity.ok().body(response);
    }
}
