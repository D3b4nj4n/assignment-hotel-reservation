package com.example.hotelreservation.service;

import com.example.creditcardpayment.model.PaymentStatusResponse;
import com.example.hotelreservation.connector.CreditCardPaymentConnector;
import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.exception.ExceptionType;
import com.example.hotelreservation.exception.ReservationException;
import com.example.hotelreservation.model.Status;
import com.example.hotelreservation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmReservationService {

    private final RoomRepository roomRepository;

    private final CreditCardPaymentConnector creditCardPaymentConnector;

    public Room confirmReservation(Room room) {

        validate(room);

        switch (room.getPaymentMode()) {
            case CASH -> room.setStatus(Status.CONFIRMED);
            case BANK_TRANSFER -> room.setStatus(Status.PENDING_PAYMENT);
            case CREDIT_CARD -> processReservationForCreditCard(room);
        }
        return roomRepository.save(room);
    }

    private void processReservationForCreditCard(Room room) {

        PaymentStatusResponse paymentStatus;
        try {
            paymentStatus = creditCardPaymentConnector.getPaymentStatus(room.getPaymentReference());
        } catch (RestClientException e) {
            throw new ReservationException(e.getLocalizedMessage(), ExceptionType.SERVER_ERROR);
        }
        if (null == paymentStatus || null == paymentStatus.getStatus()) {
            throw new RestClientException("Unable to retrieve credit card payment status");
        }

        switch (paymentStatus.getStatus()) {
            case CONFIRMED -> {
                log.info("Credit card payment status is CONFIRMED for paymentReference {}", room.getPaymentReference());
                room.setStatus(Status.CONFIRMED);
            }
            case REJECTED -> {
                log.info("Credit card payment status is REJECTED for paymentReference {}", room.getPaymentReference());
                throw new ReservationException("Credit card payment status is REJECTED for paymentReference: " + room.getPaymentReference(), ExceptionType.SERVER_ERROR);
            }
        }
    }

    private void validate(Room room) {
        LocalDate startDate = room.getStartDate();
        LocalDate endDate = room.getEndDate();
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 30) {
            throw new ReservationException("Room cannot be reserved for more than 30 days", ExceptionType.BAD_REQUEST);
        }
    }

}
