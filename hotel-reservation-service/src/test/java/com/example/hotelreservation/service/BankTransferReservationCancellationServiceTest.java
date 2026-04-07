package com.example.hotelreservation.service;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.model.PaymentMode;
import com.example.hotelreservation.model.RoomSegment;
import com.example.hotelreservation.model.Status;
import com.example.hotelreservation.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankTransferReservationCancellationServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private BankTransferReservationCancellationService cancellationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cancellationService, "deadlineDays", 2);
    }

    private Room buildPendingBankTransferRoom(String reservationId, LocalDate startDate) {

        Room room = new Room();
        room.setReservationId(reservationId);
        room.setCustomerName("Test Customer");
        room.setRoomNumber(101);
        room.setStartDate(startDate);
        room.setEndDate(startDate.plusDays(3));
        room.setRoomSegment(RoomSegment.MEDIUM);
        room.setPaymentMode(PaymentMode.BANK_TRANSFER);
        room.setStatus(Status.PENDING_PAYMENT);
        return room;
    }

    @Test
    void cancelUnpaidBankTransferReservations_cancelsEligibleReservations() {
        LocalDate today = LocalDate.now();
        LocalDate expectedDeadline = today.plusDays(2);

        Room room1 = buildPendingBankTransferRoom("RES00001", today.plusDays(1));
        Room room2 = buildPendingBankTransferRoom("RES00002", today);

        when(roomRepository.findByStatusAndPaymentModeAndStartDateLessThanEqual(
                Status.PENDING_PAYMENT, PaymentMode.BANK_TRANSFER, expectedDeadline))
                .thenReturn(List.of(room1, room2));

        cancellationService.cancelUnpaidBankTransferReservations();

        // Verify each room was saved with CANCELLED status
        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository, times(2)).save(captor.capture());

        List<Room> savedRooms = captor.getAllValues();
        assertThat(savedRooms).extracting(Room::getStatus)
                .containsOnly(Status.CANCELLED);
        assertThat(savedRooms).extracting(Room::getReservationId)
                .containsExactlyInAnyOrder("RES00001", "RES00002");
    }

    @Test
    void cancelUnpaidBankTransferReservations_doesNothing_whenNoEligibleReservations() {
        when(roomRepository.findByStatusAndPaymentModeAndStartDateLessThanEqual(
                any(), any(), any()))
                .thenReturn(Collections.emptyList());

        cancellationService.cancelUnpaidBankTransferReservations();

        verify(roomRepository, never()).save(any());
    }

    @Test
    void cancelUnpaidBankTransferReservations_setsCancelledStatus_forSingleReservation() {
        LocalDate today = LocalDate.now();
        Room room = buildPendingBankTransferRoom("RES00003", today.plusDays(1));

        when(roomRepository.findByStatusAndPaymentModeAndStartDateLessThanEqual(
                Status.PENDING_PAYMENT, PaymentMode.BANK_TRANSFER, today.plusDays(2)))
                .thenReturn(List.of(room));

        cancellationService.cancelUnpaidBankTransferReservations();

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(Status.CANCELLED);
        assertThat(captor.getValue().getReservationId()).isEqualTo("RES00003");
    }

    @Test
    void cancelUnpaidBankTransferReservations_queriesCorrectDeadline() {
        LocalDate expectedDeadline = LocalDate.now().plusDays(2);

        when(roomRepository.findByStatusAndPaymentModeAndStartDateLessThanEqual(
                any(), any(), any()))
                .thenReturn(Collections.emptyList());

        cancellationService.cancelUnpaidBankTransferReservations();

        verify(roomRepository).findByStatusAndPaymentModeAndStartDateLessThanEqual(
                Status.PENDING_PAYMENT,
                PaymentMode.BANK_TRANSFER,
                expectedDeadline);
    }

}
