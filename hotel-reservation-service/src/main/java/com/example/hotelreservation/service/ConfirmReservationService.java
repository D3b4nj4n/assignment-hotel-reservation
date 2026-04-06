package com.example.hotelreservation.service;

import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmReservationService {

    private final RoomRepository roomRepository;

    public Room confirmReservation(Room room) {
        log.info("inside confirmReservation repository");
        return roomRepository.save(room);
    }
}
