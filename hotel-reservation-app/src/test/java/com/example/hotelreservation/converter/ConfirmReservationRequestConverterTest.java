package com.example.hotelreservation.converter;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.openapi.model.ConfirmReservationRequest;
import com.example.hotelreservation.openapi.model.PaymentMode;
import com.example.hotelreservation.openapi.model.RoomSegment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfirmReservationRequestConverterTest {

    @Test
    void convert() {

        ConfirmReservationRequestConverter converter = new ConfirmReservationRequestConverter();

        ConfirmReservationRequest confirmReservationRequest = new ConfirmReservationRequest();
        confirmReservationRequest.setCustomerName("Test Customer");
        confirmReservationRequest.setRoomNumber(101);
        confirmReservationRequest.setStartDate(LocalDate.of(2026, 4, 2));
        confirmReservationRequest.setEndDate(LocalDate.of(2026, 4, 6));
        confirmReservationRequest.setRoomSegment(RoomSegment.LARGE);
        confirmReservationRequest.setPaymentMode(PaymentMode.CASH);
        confirmReservationRequest.setPaymentReference("TEST12345");

        Room room = converter.convert(confirmReservationRequest);

        assertNotNull(room);
        assertEquals("Test Customer", room.getCustomerName());
        assertEquals(101, room.getRoomNumber());
        assertEquals(LocalDate.of(2026, 4, 2), room.getStartDate());
        assertEquals(LocalDate.of(2026, 4, 6), room.getEndDate());
        assertEquals(com.example.hotelreservation.model.PaymentMode.CASH, room.getPaymentMode());
        assertEquals("TEST12345", room.getPaymentReference());
        assertEquals(com.example.hotelreservation.model.RoomSegment.LARGE, room.getRoomSegment());

    }

}
