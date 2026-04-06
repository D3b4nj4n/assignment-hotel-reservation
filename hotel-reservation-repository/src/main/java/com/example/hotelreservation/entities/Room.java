package com.example.hotelreservation.entities;

import com.example.hotelreservation.model.PaymentMode;
import com.example.hotelreservation.model.RoomSegment;
import com.example.hotelreservation.model.Status;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Room {
    @Id
    private UUID reservationId;

    private String customerName;

    private Integer roomNumber;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated
    private RoomSegment roomSegment;

    @Enumerated
    private PaymentMode paymentMode;

    private String paymentReference;

    @Enumerated
    private Status status;

}
