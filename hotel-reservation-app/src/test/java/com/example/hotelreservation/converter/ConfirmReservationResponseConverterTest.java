package com.example.hotelreservation.converter;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.model.Status;
import com.example.hotelreservation.openapi.model.ConfirmReservationResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfirmReservationResponseConverterTest {

    @Test
    void convert() {

        ConfirmReservationResponseConverter converter = new ConfirmReservationResponseConverter();

        Room room = new Room();
        room.setReservationId("A1B2C3D4");
        room.setStatus(Status.CONFIRMED);

        ConfirmReservationResponse response = converter.convert(room);

        assertNotNull(response);
        assertEquals("A1B2C3D4", response.getReservationId());
        assertEquals(com.example.hotelreservation.openapi.model.Status.CONFIRMED, response.getStatus());

    }

}
