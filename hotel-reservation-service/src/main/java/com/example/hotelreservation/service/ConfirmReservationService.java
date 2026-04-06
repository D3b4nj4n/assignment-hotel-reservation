package com.example.hotelreservation.service;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.exception.ReservationException;
import com.example.hotelreservation.model.Status;
import com.example.hotelreservation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmReservationService {

    private final RoomRepository roomRepository;

    public Room confirmReservation(Room room) {
        log.info("inside confirmReservation service layer");

        validate(room);


        switch (room.getPaymentMode()) {
            case CASH -> {
                log.info("CASH chosen as Payment mode");
                room.setStatus(Status.CONFIRMED);
            }
            case BANK_TRANSFER -> {
                log.info("BANK_TRANSFER chosen as Payment mode");
                room.setStatus(Status.PENDING_PAYMENT);
            }
            case CREDIT_CARD -> log.info("CREDIT_CARD chosen as Payment mode");
            //TODO: Call credit-card-payment-service
            //to retrieve the status of the payment. If credit payment is confirmed, then
            //confirm the room else throw an error

        }

        return roomRepository.save(room);
    }

    private void validate(Room room) {
        log.info("inside validate service layer");
        LocalDate startDate = room.getStartDate();
        LocalDate endDate = room.getEndDate();
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 30) {
            throw new ReservationException("Room cannot be reserved for more than 30 days");
        }
    }

}
