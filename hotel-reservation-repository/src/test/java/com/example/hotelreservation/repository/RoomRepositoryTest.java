package com.example.hotelreservation.repository;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.model.PaymentMode;
import com.example.hotelreservation.model.RoomSegment;
import com.example.hotelreservation.model.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableAutoConfiguration
@ContextConfiguration(classes = {RoomRepository.class})
@EntityScan("com.example.hotelreservation.entities")
class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    private Room buildRoom(String reservationId) {
        Room room = new Room();
        room.setReservationId(reservationId);
        room.setCustomerName("John Doe");
        room.setRoomNumber(101);
        room.setStartDate(LocalDate.of(2026, 5, 1));
        room.setEndDate(LocalDate.of(2026, 5, 5));
        room.setRoomSegment(RoomSegment.MEDIUM);
        room.setPaymentMode(PaymentMode.CASH);
        room.setPaymentReference("REF12345");
        room.setStatus(Status.CONFIRMED);
        return room;
    }

    @Test
    void save_persistsRoom_andCanBeRetrievedById() {
        Room room = buildRoom("aB3dE7gH");

        roomRepository.save(room);

        Optional<Room> found = roomRepository.findById("aB3dE7gH");
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerName()).isEqualTo("John Doe");
        assertThat(found.get().getRoomNumber()).isEqualTo(101);
        assertThat(found.get().getStatus()).isEqualTo(Status.CONFIRMED);
    }

    @Test
    void existsById_returnsTrue_whenRoomExists() {
        roomRepository.save(buildRoom("Zx9Qw1Lp"));

        assertThat(roomRepository.existsById("Zx9Qw1Lp")).isTrue();
    }

    @Test
    void existsById_returnsFalse_whenRoomDoesNotExist() {
        assertThat(roomRepository.existsById("00000000")).isFalse();
    }

    @Test
    void save_persistsAllFields() {
        Room room = buildRoom("kR5mT2nY");

        roomRepository.save(room);

        Room found = roomRepository.findById("kR5mT2nY").orElseThrow();
        assertThat(found.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(found.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 5));
        assertThat(found.getRoomSegment()).isEqualTo(RoomSegment.MEDIUM);
        assertThat(found.getPaymentMode()).isEqualTo(PaymentMode.CASH);
        assertThat(found.getPaymentReference()).isEqualTo("REF12345");
    }

    @Test
    void delete_removesRoom() {
        Room room = buildRoom("dE6fG8hI");
        roomRepository.save(room);

        roomRepository.deleteById("dE6fG8hI");

        assertThat(roomRepository.existsById("dE6fG8hI")).isFalse();
    }

    @Test
    void findByStatusAndPaymentModeAndStartDateLessThanEqual_returnsPendingBankTransferRoomsBeforeDeadline() {

        // BANK_TRANSFER + PENDING_PAYMENT, start date BEFORE deadline -> should be returned
        Room eligibleRoom = buildRoom("BTPEND001");
        eligibleRoom.setPaymentMode(PaymentMode.BANK_TRANSFER);
        eligibleRoom.setStatus(Status.PENDING_PAYMENT);
        eligibleRoom.setStartDate(LocalDate.of(2026, 5, 1));
        roomRepository.save(eligibleRoom);

        // BANK_TRANSFER + PENDING_PAYMENT, start date AFTER deadline -> should NOT be returned
        Room futureRoom = buildRoom("BTPEND002");
        futureRoom.setPaymentMode(PaymentMode.BANK_TRANSFER);
        futureRoom.setStatus(Status.PENDING_PAYMENT);
        futureRoom.setStartDate(LocalDate.of(2026, 5, 10));
        roomRepository.save(futureRoom);

        // BANK_TRANSFER + CONFIRMED -> should NOT be returned
        Room confirmedRoom = buildRoom("BTCONF001");
        confirmedRoom.setPaymentMode(PaymentMode.BANK_TRANSFER);
        confirmedRoom.setStatus(Status.CONFIRMED);
        confirmedRoom.setStartDate(LocalDate.of(2026, 5, 1));
        roomRepository.save(confirmedRoom);

        // CASH + PENDING_PAYMENT -> should NOT be returned
        Room cashRoom = buildRoom("CASHPD001");
        cashRoom.setPaymentMode(PaymentMode.CASH);
        cashRoom.setStatus(Status.PENDING_PAYMENT);
        cashRoom.setStartDate(LocalDate.of(2026, 5, 1));
        roomRepository.save(cashRoom);

        LocalDate deadline = LocalDate.of(2026, 5, 3); // +2 days from today (2026-05-01)

        List<Room> result = roomRepository.findByStatusAndPaymentModeAndStartDateLessThanEqual(
                Status.PENDING_PAYMENT, PaymentMode.BANK_TRANSFER, deadline);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getReservationId()).isEqualTo("BTPEND001");
    }

    @Test
    void findByStatusAndPaymentModeAndStartDateLessThanEqual_returnsEmpty_whenNoPendingBankTransferRooms() {
        List<Room> result = roomRepository.findByStatusAndPaymentModeAndStartDateLessThanEqual(
                Status.PENDING_PAYMENT, PaymentMode.BANK_TRANSFER, LocalDate.of(2026, 5, 3));

        assertThat(result).isEmpty();
    }


}