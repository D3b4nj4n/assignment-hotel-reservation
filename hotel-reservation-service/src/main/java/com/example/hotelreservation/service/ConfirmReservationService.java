package com.example.hotelreservation.service;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfirmReservationService {

    private final RoomRepository roomRepository;

    public Room confirmReservation(Room room) {
        return roomRepository.save(room);
    }
}
