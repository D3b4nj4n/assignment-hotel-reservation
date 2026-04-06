package com.example.hotelreservation.converter;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.openapi.model.ConfirmReservationResponse;
import com.example.hotelreservation.openapi.model.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConfirmReservationResponseConverter implements Converter<Room, ConfirmReservationResponse> {

    @Override
    public ConfirmReservationResponse convert(Room source) {

        log.info("inside response converter");
        ConfirmReservationResponse response = new ConfirmReservationResponse();
        response.setReservationId(source.getReservationId());
        response.setStatus(Status.valueOf(source.getStatus().name()));
        return response;

    }
}
