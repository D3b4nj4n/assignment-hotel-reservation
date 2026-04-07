package com.example.hotelreservation.repository;

import com.example.hotelreservation.entities.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE) // to block other threads until the transaction commits
    Optional<Room> findByReservationId(String reservationId);
}
