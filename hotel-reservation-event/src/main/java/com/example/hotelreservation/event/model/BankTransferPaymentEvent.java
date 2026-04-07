package com.example.hotelreservation.event.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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

    public String getE2eId() {
        if (null == transactionDescription || transactionDescription.length() < 10) {
            return null;
        }
        return transactionDescription.substring(0, 10);
    }

    public String getReservationIdFromDescription() {
        if (null == transactionDescription || transactionDescription.length() < 10) {
            return null;
        }
        return transactionDescription.substring(11).trim();
    }

}
