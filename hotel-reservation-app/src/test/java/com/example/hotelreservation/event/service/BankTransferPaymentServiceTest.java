package com.example.hotelreservation.event.service;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.event.model.BankTransferPaymentEvent;
import com.example.hotelreservation.exception.ReservationException;
import com.example.hotelreservation.model.PaymentMode;
import com.example.hotelreservation.model.Status;
import com.example.hotelreservation.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankTransferPaymentServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private BankTransferPaymentService bankTransferPaymentService;

    private BankTransferPaymentEvent event;
    private Room room;

    @BeforeEach
    void setUp() {

        event = new BankTransferPaymentEvent();
        event.setPaymentId("pay-001");
        event.setDebtorAccountNumber("NL12BANK0123456789");
        event.setAmountReceived("150.00");
        event.setTransactionDescription("E2EREFERENCE RES12345");

        room = new Room();
        room.setReservationId("RES12345");
        room.setPaymentMode(PaymentMode.BANK_TRANSFER);
        room.setStatus(Status.PENDING_PAYMENT);
    }

    @Test
    void shouldConfirmReservationSuccessfully() {

        when(roomRepository.findByReservationId("RES12345")).thenReturn(Optional.of(room));

        bankTransferPaymentService.processPaymentUpdate(event);

        verify(roomRepository).save(room);
        assertEquals(Status.CONFIRMED, room.getStatus());
    }

    @Test
    void shouldThrowWhenTransactionDescriptionIsBlank() {

        event.setTransactionDescription(null);

        assertThrows(ReservationException.class, () -> bankTransferPaymentService.processPaymentUpdate(event));
        verifyNoInteractions(roomRepository);
    }

    @Test
    void shouldThrowWhenTransactionDescriptionIsTooShort() {

        event.setTransactionDescription("short");

        assertThrows(ReservationException.class, () -> bankTransferPaymentService.processPaymentUpdate(event));
        verifyNoInteractions(roomRepository);
    }

    @Test
    void shouldThrowWhenReservationNotFound() {

        when(roomRepository.findByReservationId("RES12345")).thenReturn(Optional.empty());

        assertThrows(ReservationException.class, () -> bankTransferPaymentService.processPaymentUpdate(event));
        verify(roomRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPaymentModeIsNotBankTransfer() {

        room.setPaymentMode(PaymentMode.CREDIT_CARD);
        when(roomRepository.findByReservationId("RES12345")).thenReturn(Optional.of(room));

        assertThrows(ReservationException.class, () -> bankTransferPaymentService.processPaymentUpdate(event));
        verify(roomRepository, never()).save(any());
    }

    @Test
    void shouldSkipWhenReservationAlreadyConfirmed() {

        room.setStatus(Status.CONFIRMED);
        when(roomRepository.findByReservationId("RES12345")).thenReturn(Optional.of(room));

        bankTransferPaymentService.processPaymentUpdate(event);

        verify(roomRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenReservationIsCancelled() {

        room.setStatus(Status.CANCELLED);
        when(roomRepository.findByReservationId("RES12345")).thenReturn(Optional.of(room));

        assertThrows(ReservationException.class, () -> bankTransferPaymentService.processPaymentUpdate(event));
        verify(roomRepository, never()).save(any());
    }
}