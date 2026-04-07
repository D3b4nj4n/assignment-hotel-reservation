package com.example.hotelreservation.service;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.model.PaymentMode;
import com.example.hotelreservation.model.Status;
import com.example.hotelreservation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of the scheduler to cancel reservation if amount is not paid within 2 days of start date
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankTransferReservationCancellationService {

    private final RoomRepository roomRepository;

    @Value("${reservation.cancellation.deadlineDays:2}")
    private int deadlineDays;

    /**
     * Scheduled service that automatically cancels the BANK_TRANSFER reservation
     * when the full payment amount has not been received at least 2 days before the reservation start date
     *
     * <p>This scheduled job runs every day at midnight (as configured via cron) and cancels any reservation that:
     * <ul>
     *     <li>has PaymentMode {@code BANK_TRANSFER}</li>
     *     <li>still has Status {@code PENDING_PAYMENT}</li>
     *     <li>has a start date that is today or earlier, i.e., 2-day deadline has passed</li>
     * </ul>
     */
    @Scheduled(cron = "${reservation.cancellation.cron:0 0 0 * * *}")
    @Transactional
    public void cancelUnpaidBankTransferReservations() {

        LocalDate deadline = LocalDate.now().plusDays(deadlineDays);

        log.info("Running auto-cancellation job for unpaid BANK_TRANSFER reservations. Cancelling reservations " +
                "with startDate on or before: {}", deadline);

        List<Room> pendingReservations = roomRepository.findByStatusAndPaymentModeAndStartDateLessThanEqual(
                Status.PENDING_PAYMENT, PaymentMode.BANK_TRANSFER, deadline);

        if (pendingReservations.isEmpty()) {
            log.info("No unpaid BANK_TRANSFER reservations found for auto-cancellation");
            return;
        }

        log.info("Found {} unpaid BANK_TRANSFER reservation(s) to cancel.", pendingReservations.size());

        pendingReservations.forEach(room -> {
            room.setStatus(Status.CANCELLED);
            roomRepository.save(room);
            log.info("Reservation '{}' for customer '{}' with startDate '{}' has been automatically CANCELLED " +
                            "due to missing BANK_TRANSFER payment {} days before start date.",
                    room.getReservationId(), room.getCustomerName(), room.getStartDate(), deadlineDays);
        });

        log.info("Auto-cancellation job completed. {} reservation(s) has been automatically CANCELLED.", pendingReservations.size());
    }

}
