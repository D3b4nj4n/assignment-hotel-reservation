# hotel-reservation-assignment
* Need to update this section

# Assignment Details:

Marvel hospitality management corporation has a microservice based IT and one of the
microservice room-reservation-service is to manage the reservations.
Your task is to develop following two functionalities in a Spring Boot application:

1. Rest end point to confirm a room reservation.
    * If mode of payment is cash, room must be confirmed immediately
    * If mode of payment is credit card, call rest api `credit-card-payment-service`
      to retrieve the status of the payment. If credit payment is confirmed, then
      confirm the room, else throw an error
    * If mode of payment is bank transfer, room must be booked with pending
      payment status.
2. Consume `bank-transfer-payment-update` topic to listen to payment updates and
   confirm the booking if payment is confirmed.
3. Automatically cancel the reservation, If the payment is done using a bank transfer
   and total amount is not received 2 day before the reservation start date.

## Rest Endpoint Details
* Confirm room reserva.on
    * Request Data:
        * Name of Customer
        * Room Number
        * Reservation start and end date
        * Room segment
            * Small
            * Medium
            * Large
            * Extra Large
        * Mode of Payment
            * Cash
            * Bank Transfer
            * Credit Card
        * Payment Reference

    * Response:
        * Reservation Id
        * Reservation status
            * PENDING_PAYMENT
            * CONFIRMED
            * CANCELLED

    * Validations:
        * A room cannot be reserved for more than 30 days.

# Extra information
## Event driven architecture

Marvel hospitality uses an event broker for implementing event driven architecture. A topic
bank-transfer-payment-update provides the events related to the payments received on
hotels account. Following is data present in the event

| Attribute              | Description                                                                                                                           |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| paymentId              | Unique id of the payment                                                                                                              |
| debtorAccountnumber    | Account number from which payment was done                                                                                            |
| amountReceived         | Payment amount                                                                                                                        |
| transactionDescription | Transaction Description.<br/> Format < E2E unique id(10 character)> <reservationId (8 characters)> <br/> Example: 1401541457 P4145478 |


# credit-card-payment-service specs
open api specs credit-card-payment-service.yaml is provided along with the assignment.