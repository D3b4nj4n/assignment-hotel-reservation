package com.example.hotelreservation.event.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a bank transfer payment event received from the Kafka topic.
 *
 * <p>The {@code transactionDescription} field carries a structured value used to link the payment to a reservation.
 * The expected format is:
 * <pre>
 *   {@code <10-char e2eId> <8-char reservationId>}
 * </pre>
 * For example: {@code E2EREFERAB RES12345}
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankTransferPaymentEvent {

    private String paymentId;

    private String debtorAccountNumber;

    private String amountReceived;

    private String transactionDescription;

    /**
     * Extracts the e2e ID from the first 10 characters of {@link #transactionDescription}.
     *
     * @return the e2e ID, or {@code null} if the description is absent or shorter than 10 characters
     */
    public String getE2eId() {

        if (null == transactionDescription || transactionDescription.length() < 10) {
            return null;
        }
        return transactionDescription.substring(0, 10);
    }

    /**
     * Extracts the reservation ID from {@link #transactionDescription} by taking everything after the 11th character
     *
     * @return the reservation ID, or {@code null} if the description is absent or shorter than 10 characters
     */
    public String getReservationIdFromDescription() {

        if (null == transactionDescription || transactionDescription.length() < 10) {
            return null;
        }
        return transactionDescription.substring(11).trim();
    }

}
