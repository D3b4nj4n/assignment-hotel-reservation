package com.example.hotelreservation.model;

import java.time.LocalDate;
import java.util.UUID;

public class Room {

    //TODO: remove if not needed
    private UUID reservationId;
    private String customerName;
    private Integer roomNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private RoomSegment roomSegment;
    private PaymentMode paymentMode;
    private String paymentReference;

}
