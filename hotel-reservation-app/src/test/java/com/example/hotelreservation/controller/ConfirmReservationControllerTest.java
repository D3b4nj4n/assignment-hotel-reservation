package com.example.hotelreservation.controller;

import com.example.hotelreservation.converter.ConfirmReservationRequestConverter;
import com.example.hotelreservation.converter.ConfirmReservationResponseConverter;
import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.openapi.model.ConfirmReservationRequest;
import com.example.hotelreservation.openapi.model.ConfirmReservationResponse;
import com.example.hotelreservation.service.ConfirmReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmReservationControllerTest {

    @Mock
    private ConfirmReservationService confirmReservationService;

    @Mock
    private ConfirmReservationRequest request;

    @Mock
    private ConfirmReservationRequestConverter requestConverter;

    @Mock
    private ConfirmReservationResponse response;

    @Mock
    private ConfirmReservationResponseConverter responseConverter;

    @Mock
    private Room room;

    @InjectMocks
    private ConfirmReservationController confirmReservationController;

    @Test
    void confirmReservation_Success() {

        when(requestConverter.convert(request)).thenReturn(room);
        when(confirmReservationService.confirmReservation(any(Room.class))).thenReturn(room);
        when(responseConverter.convert(room)).thenReturn(response);

        ResponseEntity<ConfirmReservationResponse> result =
                confirmReservationController.confirmReservation("TraceId123", request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());

        verify(requestConverter).convert(request);
        verify(confirmReservationService).confirmReservation(any(Room.class));
        verify(responseConverter).convert(room);

    }

}
