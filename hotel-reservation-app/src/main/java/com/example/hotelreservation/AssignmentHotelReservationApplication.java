package com.example.hotelreservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.hotelreservation.entities")
@EnableJpaRepositories("com.example.hotelreservation.repository")
public class AssignmentHotelReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssignmentHotelReservationApplication.class, args);
    }

}
