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
}