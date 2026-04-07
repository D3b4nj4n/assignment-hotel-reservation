package com.example.hotelreservation.repository;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.model.PaymentMode;
import com.example.hotelreservation.model.Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Room entity
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    /**
     * Find reservation by the reservationId
     * If no reservation is found, provide null
     *
     * @param reservationId unique id of the reservation to be used as search filter
     * @return {@link Optional<Room>} optional Room entity found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // to block other threads until the transaction commits
    Optional<Room> findByReservationId(String reservationId);

    /**
     * Find all reservation with given status (PENDING_PAYMENT) and PaymentMode (BANK_TRANSFER)
     * whose startDate is on or before the given Deadline date (calculated in service layer)
     * <p>
     * It is used by auto-cancellation scheduler to cancel reservation where full payment was not received
     * at least 2 days before the reservation startDate
     *
     * @param status       status of the reservation
     * @param paymentMode  mode of payment used
     * @param deadlineDate deadline for cancellation
     * @return {@link List<Room>} list of Room entities matching the criteria
     */
    List<Room> findByStatusAndPaymentModeAndStartDateLessThanEqual(
            Status status, PaymentMode paymentMode, LocalDate deadlineDate);
}
