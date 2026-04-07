package com.example.hotelreservation.event.service;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.event.model.BankTransferPaymentEvent;
import com.example.hotelreservation.exception.ExceptionType;
import com.example.hotelreservation.exception.ReservationException;
import com.example.hotelreservation.model.PaymentMode;
import com.example.hotelreservation.model.Status;
import com.example.hotelreservation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for confirming hotel reservations upon receipt of a bank transfer payment event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankTransferPaymentService {

    private final RoomRepository roomRepository;

    /**
     * Processes a {@link BankTransferPaymentEvent} and confirms the associated reservation.
     *
     * <p>The following validations are applied in order:
     * <ol>
     *   <li>The {@code transactionDescription} must contain a reservation ID;
     *       otherwise a {@link ReservationException} with {@code BAD_REQUEST} is thrown.</li>
     *   <li>A {@link Room} with the extracted reservation ID must exist;
     *       otherwise a {@link ReservationException} with {@code NOT_FOUND} is thrown.</li>
     *   <li>The room's payment mode must be {@link PaymentMode#BANK_TRANSFER};
     *       otherwise a {@link ReservationException} with {@code BAD_REQUEST} is thrown.</li>
     *   <li>If the reservation is already {@link Status#CONFIRMED}, the event is ignored.</li>
     *   <li>If the reservation is {@link Status#CANCELLED}, a {@link ReservationException}
     *       with {@code BAD_REQUEST} is thrown.</li>
     * </ol>
     *
     * @param event the bank transfer payment event to process
     * @throws ReservationException if the event is invalid or the reservation cannot be confirmed
     */
    @Transactional
    public void processPaymentUpdate(BankTransferPaymentEvent event) {

        log.info("Bank Transfer Payment Event Received, paymentId: {}, debtorAccount:{}, amount:{}",
                event.getPaymentId(), event.getDebtorAccountNumber(), event.getAmountReceived());

        String reservationId = event.getReservationIdFromDescription();
        if (StringUtils.isBlank(reservationId)) {
            log.error("Invalid transactionDescription in payment event: '{}', Expected format: " +
                    "'<10-char E2E id> <8-char reservationId>'. Event will be skipped.", event.getTransactionDescription());
            throw new ReservationException(
                    "Invalid transactionDescription format: " + event.getTransactionDescription(),
                    ExceptionType.BAD_REQUEST);
        }

        //read if the room is present
        Room room = roomRepository.findByReservationId(reservationId)
                .orElseThrow(() -> {
                    log.error("No reservation found for reservationId: '{}' from payment event paymentId: '{}'",
                            reservationId, event.getPaymentId());
                    return new ReservationException("Reservation not found for reservationId: " + reservationId,
                            ExceptionType.NOT_FOUND);
                });

        //check if paymentMode is BANK_TRANSFER
        if (PaymentMode.BANK_TRANSFER != room.getPaymentMode()) {
            log.warn("Reservation: '{}' has paymentMode: '{}', expected BANK_TRANSFER. Skipping confirmation.",
                    reservationId, room.getPaymentMode());
            throw new ReservationException(
                    "Reservation: '{}' is not a BANK_TRANSFER reservation.",
                    ExceptionType.BAD_REQUEST);
        }

        //check if status is CONFIRMED
        if (Status.CONFIRMED == room.getStatus()) {
            log.warn("Reservation: '{}' is already CONFIRMED. Duplicate payment event paymentId: '{}' to be ignored.",
                    reservationId, event.getPaymentId());
            return;
        }

        //check if the status is CANCELLED
        if (Status.CANCELLED == room.getStatus()) {
            log.warn("Reservation: '{}' is CANCELLED. Payment event paymentId: '{}' cannot confirm a cancelled reservation.",
                    reservationId, event.getPaymentId());
            throw new ReservationException(
                    "Cannot confirm a CANCELLED reservation: " + reservationId,
                    ExceptionType.BAD_REQUEST);
        }

        //modify the status
        room.setStatus(Status.CONFIRMED);
        //write the new status to db
        roomRepository.save(room);

        log.info("Reservation: '{}' successfully CONFIRMED via BANK_TRANSFER Payment Mode, paymentId: '{}', e2eId: '{}'",
                reservationId, event.getPaymentId(), event.getE2eId());
    }

}
