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

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Service Implementation class that handles the logic for processing the room reservation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmReservationService {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoomRepository roomRepository;

    private final CreditCardPaymentConnector creditCardPaymentConnector;

    /**
     * The service implementation to confirmt he room reservation
     * <p>
     * Avoiding @Transactional here. This method calls external API (CreditCardPaymentApi) mid-flow.
     * Wrapping that in a transaction would hold an open DB connection during the entire network round-trip,
     * which is wasteful and can exhaust the connection pool (default used with in-mem H2 database).
     *
     * @param room the Room entity
     * @return {@link Room} updated Room entity based on reservation details
     */
    public Room confirmReservation(Room room) {

        validate(room);

        room.setReservationId(generateUniqueReservationId());
        switch (room.getPaymentMode()) {
            case CASH -> room.setStatus(Status.CONFIRMED); //immediately confirm the reservation
            case BANK_TRANSFER ->
                    room.setStatus(Status.PENDING_PAYMENT); //set status to PENDING_PAYMENT, handle the processing based on event listener separately
            case CREDIT_CARD ->
                    processReservationForCreditCard(room); //call credt-card-payment-api and process accordingly
        }
        return roomRepository.save(room);
    }

    /**
     * Generates a unique reservationId
     * based on the pattern that it should be a random 8-character alphanumeric string
     *
     * @return {@link String} unique reservationId
     */
    private String generateUniqueReservationId() {
        String id;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            id = sb.toString();
        } while (roomRepository.existsById(id));
        return id;
    }

    /**
     * Processes the reservation by calling credit-card-payment-api
     * If any issue observed while consuming the api, throw applicable exception
     * Otherwise, process the reservation based on payment status:
     * <ul>
     *     <li>If paymentStatus is CONFIRMED, then confirm the reservation</li>
     *     <li>If paymentStatus is REJECTED, then thrown applicable exception</li>
     * </ul>
     *
     * @param room the Room entity to process
     */
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

    /**
     * Validates the period of reservation
     * If the endDate is more than 30 days of the startDate, then an exception is thrown
     *
     * @param room the Room entity to be validated
     */
    private void validate(Room room) {
        LocalDate startDate = room.getStartDate();
        LocalDate endDate = room.getEndDate();
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 30) {
            throw new ReservationException("Room cannot be reserved for more than 30 days", ExceptionType.BAD_REQUEST);
        }
    }

}
