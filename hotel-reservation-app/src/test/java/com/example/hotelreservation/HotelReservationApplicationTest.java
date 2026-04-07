package com.example.hotelreservation;

import com.example.hotelreservation.connector.CreditCardPaymentConnector;
import com.example.hotelreservation.service.ConfirmReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HotelReservationApplicationTest {

    @MockitoBean
    private CreditCardPaymentConnector creditCardPaymentConnector;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {

        assertThat(applicationContext).isNotNull();
    }

    @Test
    void confirmReservationService_beanIsPresent() {

        assertThat(applicationContext.getBean(ConfirmReservationService.class)).isNotNull();
    }

    @Test
    void applicationClass_isAnnotatedWithSpringBootApplication() {

        assertThat(HotelReservationApplication.class)
                .hasAnnotation(SpringBootApplication.class);
    }

    @Test
    void applicationClass_isAnnotatedWithEnableScheduling() {

        assertThat(HotelReservationApplication.class)
                .hasAnnotation(EnableScheduling.class);
    }

    @Test
    void applicationClass_entityScanTargetsCorrectPackage() {

        EntityScan entityScan = HotelReservationApplication.class.getAnnotation(EntityScan.class);
        assertThat(entityScan).isNotNull();
        assertThat(entityScan.value()).contains("com.example.hotelreservation.entities");
    }

    @Test
    void applicationClass_enableJpaRepositoriesTargetsCorrectPackage() {

        EnableJpaRepositories enableJpaRepositories =
                HotelReservationApplication.class.getAnnotation(EnableJpaRepositories.class);
        assertThat(enableJpaRepositories).isNotNull();
        assertThat(enableJpaRepositories.value()).contains("com.example.hotelreservation.repository");
    }

}
