package com.example.hotelreservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan("com.example.hotelreservation.entities")
@EnableJpaRepositories("com.example.hotelreservation.repository")
@EnableScheduling
public class HotelReservationApplication {

    public static void main(String[] args) {

        SpringApplication.run(HotelReservationApplication.class, args);
    }

}
