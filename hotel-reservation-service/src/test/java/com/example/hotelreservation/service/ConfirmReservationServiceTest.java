package com.example.hotelreservation.service;

import com.example.creditcardpayment.model.PaymentStatusResponse;
import com.example.hotelreservation.connector.CreditCardPaymentConnector;
import com.example.hotelreservation.entities.Room;
import com.example.hotelreservation.exception.ExceptionType;
import com.example.hotelreservation.exception.ReservationException;
import com.example.hotelreservation.model.PaymentMode;
import com.example.hotelreservation.model.RoomSegment;
import com.example.hotelreservation.model.Status;
import com.example.hotelreservation.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmReservationServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private CreditCardPaymentConnector creditCardPaymentConnector;

    @InjectMocks
    private ConfirmReservationService confirmReservationService;

    private Room buildRoom(PaymentMode paymentMode, int durationDays) {

        Room room = new Room();
        room.setCustomerName("John Doe");
        room.setRoomNumber(101);
        room.setRoomSegment(RoomSegment.MEDIUM);
        room.setPaymentMode(paymentMode);
        room.setStartDate(LocalDate.now());
        room.setEndDate(LocalDate.now().plusDays(durationDays));
        room.setPaymentReference("PAY-REF-001");
        return room;
    }

    @Test
    void confirmReservation_setsCONFIRMED_forCashPayment() {

        Room room = buildRoom(PaymentMode.CASH, 3);
        when(roomRepository.existsById(anyString())).thenReturn(false);
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Room result = confirmReservationService.confirmReservation(room);

        assertThat(result.getStatus()).isEqualTo(Status.CONFIRMED);
        assertThat(result.getReservationId()).isNotBlank();
    }

    @Test
    void confirmReservation_setsPENDING_PAYMENT_forBankTransfer() {

        Room room = buildRoom(PaymentMode.BANK_TRANSFER, 5);
        when(roomRepository.existsById(anyString())).thenReturn(false);
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Room result = confirmReservationService.confirmReservation(room);

        assertThat(result.getStatus()).isEqualTo(Status.PENDING_PAYMENT);
        assertThat(result.getReservationId()).isNotBlank();
        verifyNoInteractions(creditCardPaymentConnector);
    }

    @Test
    void confirmReservation_setsCONFIRMED_whenCreditCardPaymentIsConfirmed() {

        Room room = buildRoom(PaymentMode.CREDIT_CARD, 2);
        when(roomRepository.existsById(anyString())).thenReturn(false);
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setStatus(PaymentStatusResponse.StatusEnum.CONFIRMED);
        when(creditCardPaymentConnector.getPaymentStatus("PAY-REF-001")).thenReturn(response);

        Room result = confirmReservationService.confirmReservation(room);

        assertThat(result.getStatus()).isEqualTo(Status.CONFIRMED);
        verify(creditCardPaymentConnector).getPaymentStatus("PAY-REF-001");
    }


    @Test
    void confirmReservation_throwsReservationException_whenCreditCardPaymentIsRejected() {

        Room room = buildRoom(PaymentMode.CREDIT_CARD, 2);
        when(roomRepository.existsById(anyString())).thenReturn(false);

        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setStatus(PaymentStatusResponse.StatusEnum.REJECTED);
        when(creditCardPaymentConnector.getPaymentStatus("PAY-REF-001")).thenReturn(response);

        assertThatThrownBy(() -> confirmReservationService.confirmReservation(room))
                .isInstanceOf(ReservationException.class)
                .satisfies(ex -> assertThat(((ReservationException) ex).getType())
                        .isEqualTo(ExceptionType.SERVER_ERROR));

        verify(roomRepository, never()).save(any());
    }

    @Test
    void confirmReservation_throwsException_whenCreditCardPaymentStatusIsNull() {

        Room room = buildRoom(PaymentMode.CREDIT_CARD, 2);
        when(roomRepository.existsById(anyString())).thenReturn(false);
        when(creditCardPaymentConnector.getPaymentStatus("PAY-REF-001")).thenReturn(null);

        assertThatThrownBy(() -> confirmReservationService.confirmReservation(room))
                .isInstanceOf(RuntimeException.class);

        verify(roomRepository, never()).save(any());
    }


    @Test
    void confirmReservation_throwsReservationException_whenReservationDaysExceeds30Days() {

        Room room = buildRoom(PaymentMode.CASH, 31);

        assertThatThrownBy(() -> confirmReservationService.confirmReservation(room))
                .isInstanceOf(ReservationException.class)
                .satisfies(ex -> assertThat(((ReservationException) ex).getType())
                        .isEqualTo(ExceptionType.BAD_REQUEST));

        verifyNoInteractions(roomRepository, creditCardPaymentConnector);
    }


    @Test
    void confirmReservation_generatesUniqueReservationId_retryingOnCollision() {

        Room room = buildRoom(PaymentMode.CASH, 3);

        when(roomRepository.existsById(anyString()))
                .thenReturn(true, true, false);
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Room result = confirmReservationService.confirmReservation(room);

        assertThat(result.getReservationId()).isNotBlank();

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(roomRepository, times(3)).existsById(idCaptor.capture());
        // All generated IDs are 8-character alphanumeric strings
        idCaptor.getAllValues().forEach(id -> assertThat(id).matches("[A-Za-z0-9]{8}"));
    }

    @Test
    void confirmReservation_savesRoomExactlyOnce() {

        Room room = buildRoom(PaymentMode.BANK_TRANSFER, 7);
        when(roomRepository.existsById(anyString())).thenReturn(false);
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        confirmReservationService.confirmReservation(room);

        verify(roomRepository, times(1)).save(room);
    }

}
