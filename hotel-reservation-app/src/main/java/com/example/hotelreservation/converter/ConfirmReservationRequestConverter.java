package com.example.hotelreservation.converter;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.model.PaymentMode;
import com.example.hotelreservation.model.RoomSegment;
import com.example.hotelreservation.openapi.model.ConfirmReservationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class ConfirmReservationRequestConverter implements Converter<ConfirmReservationRequest, Room> {

    @Override
    public Room convert(ConfirmReservationRequest source) {

        log.info("inside request converter");

        Room room = new Room();
        room.setReservationId(UUID.randomUUID());
        room.setCustomerName(source.getCustomerName());
        room.setRoomNumber(source.getRoomNumber());
        room.setStartDate(source.getStartDate());
        room.setEndDate(source.getEndDate());
        room.setRoomSegment(RoomSegment.valueOf(String.valueOf(source.getRoomSegment())));
        room.setPaymentMode(PaymentMode.valueOf(String.valueOf(source.getPaymentMode())));
        room.setPaymentReference(source.getPaymentReference());

        return room;

    }
}
