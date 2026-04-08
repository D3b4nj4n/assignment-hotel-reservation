# Hotel Reservation Service

A Spring Boot service for managing hotel room reservations for Marvel Hospitality Management Corporation. It handles reservation confirmation via REST, payment processing through Credit Card and Kafka-based Bank Transfer events, and automatic cancellation of unpaid reservations.

---

## Table of Contents

1. [Requirements](#1-requirements)
2. [Project Modules](#2-project-modules)
3. [Architecture Overview](#3-architecture-overview)
4. [How to Build and Start the Application](#4-how-to-build-and-start-the-application)
5. [Credit Card Payment API](#5-credit-card-payment-api)
6. [Kafka Setup](#6-kafka-setup)
7. [Database Management](#7-database-management)
8. [Testing the Functionalities](#8-testing-the-functionalities)
9. [Running the Tests](#9-running-the-tests)
10. [Configuration Reference](#10-configuration-reference)

---

## 1. Requirements

| Requirement                          | Version / Details                                                         |
|--------------------------------------|---------------------------------------------------------------------------|
| Java                                 | 21+                                                                       |
| Maven                                | 3.8+ (Maven Wrapper included)                                             |
| Apache Kafka                         | 3.x (for running locally)                                                 |
| OpenAPI Spec                         | 3.0                                                                       |
| Other tools like Postman mock server | Required if using Postman mock server for running Credit Card API locally |

No separate database installation is needed — the application uses an **H2 in-memory database** by default.

---

## 2. Project Modules

The project is a Maven multi-module build. Each module has a single, well-defined responsibility.

```
assignment-hotel-reservation/
├── hotel-reservation-model        # Enums shared across all modules
├── hotel-reservation-exception    # Custom exception hierarchy
├── hotel-reservation-repository   # JPA entities and repositories (for in-memory H2 database)
├── hotel-reservation-connector    # REST client for the Credit Card Payment API (uses Spring RestClient)
├── hotel-reservation-service      # Core business logic
├── hotel-reservation-openapi      # OpenAPI-generated REST controller interface (uses OAS 3.0)
└── hotel-reservation-app          # Spring Boot entry point, Kafka listener, converters
```

### Module Details

#### `hotel-reservation-model`
Contains the shared domain enumerations used throughout the application:
- `PaymentMode` — `CASH`, `BANK_TRANSFER`, `CREDIT_CARD`
- `RoomSegment` — `SMALL`, `MEDIUM`, `LARGE`, `EXTRA_LARGE`
- `Status` — `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`

#### `hotel-reservation-exception`
Defines the custom exception hierarchy:
- `ReservationException` — base exception; carries an `ExceptionType` that maps to an HTTP status code (400, 404, 409, 500, 502, 503, 504, etc.)
- `CreditCardPaymentConnectorException` — extends `ReservationException`; distinguishes timeout (504) from service unavailable (503)

#### `hotel-reservation-repository`
Contains JPA entities and repositories (for in-memory H2 database)
- `Room` — JPA entity persisted to H2. Primary key is an 8-character alphanumeric `reservationId`.
- `RoomRepository` — Spring Data JPA repository with a `PESSIMISTIC_WRITE` lock on lookups to prevent concurrent update conflicts.

#### `hotel-reservation-connector`
Contains the REST client for the Credit Card Payment API and its configurations to auto-generate models using OpenAPI generator.
- `CreditCardPaymentConnector` — calls the external Credit Card Payment API using Spring's `RestClient`.
- Applies a `RetryTemplate` with exponential backoff (up to 5 attempts, 500 ms → 10 s).
- Propagates the `Trace-Id` header to the external service using slf4j Mapped Diagnostic Context.
- The API client code is **generated** from `Assignment02_creditcardpayment_api.yaml` using the OpenAPI Generator Maven plugin.


**Note**: Some irregularities were present in `Assignment02_creditcardpayment_api.yaml` in enum and description. 
Modified them to properly generate the required clients and models.

#### `hotel-reservation-service`
Contains the core business implementation logic
- `ConfirmReservationService` — validates the reservation (max 30 days), generates a unique `reservationId`, and routes to the correct payment flow (Cash / Bank Transfer / Credit Card).
- `BankTransferReservationCancellationService` — a scheduled job (cron, daily at midnight) that auto-cancels `PENDING_PAYMENT` bank-transfer reservations whose start date is within 2 days.

#### `hotel-reservation-openapi`
Generates the REST controller interface from `RoomReservationAPI.yaml`. The application module implements this interface in `ConfirmReservationController`.

#### `hotel-reservation-app`
The runnable Spring Boot application. Contains:
- `HotelReservationApplication` — main class with `@SpringBootApplication`, `@EnableScheduling`
- `ConfirmReservationController` — implements the generated API interface
- `TraceIdFilter` — servlet filter that extracts or auto-generates a `Trace-Id` UUID and stores it in the MDC for structured logging
- `BankTransferPaymentListener` — `@KafkaListener` on the `bank-transfer-payment-update` topic
- `BankTransferPaymentService` — processes incoming Kafka events to confirm reservations
- `ReservationExceptionHandler` — `@ControllerAdvice` that maps exceptions to HTTP error responses

---

## 3. Architecture Overview

```
Client
  │
  ▼ POST /roomreservationapi/v1/confirm-reservation
TraceIdFilter ──► ConfirmReservationController
                        │
                        ▼
               ConfirmReservationService
                  ├── CASH ──────────────────────► status = CONFIRMED → save to H2
                  ├── BANK_TRANSFER ──────────────► status = PENDING_PAYMENT → save to H2
                  └── CREDIT_CARD ───────────────► CreditCardPaymentConnector (with retry)
                                                        ├── CONFIRMED → save to H2
                                                        └── REJECTED  → throw exception

Kafka Topic: bank-transfer-payment-update
  │
  ▼
BankTransferPaymentListener
  │
  ▼
BankTransferPaymentService ──► find reservation → set CONFIRMED → save to H2

Scheduled (daily midnight)
  │
  ▼
BankTransferReservationCancellationService ──► cancel overdue PENDING_PAYMENT reservations
```

---

## 4. How to Build and Start the Application

### Step 1 — Clone and build

```bash
git clone https://github.com/D3b4nj4n/assignment-hotel-reservation.git
cd assignment-hotel-reservation
./mvnw clean install
```

This compiles all modules, runs unit tests, and generates OpenAPI client/server code.

### Step 2 — Start a local Kafka broker

See [Section 6 — Kafka Setup](#6-kafka-setup).

### Step 3 — Configure the Credit Card Payment API URL

See [Section 5 — Credit Card Payment API](#5-credit-card-payment-api).

### Step 4 — Run the application

```bash
./mvnw spring-boot:run -f hotel-reservation-app/pom.xml
```

Or run the JAR directly after building:

```bash
java -jar hotel-reservation-app/target/hotel-reservation-app-*.jar
```

The application starts on **port 8080** with context path `/roomreservationapi/v1`.

Base URL: `http://localhost:8080/roomreservationapi/v1`

---

## 5. Credit Card Payment API

The `hotel-reservation-connector` module calls an external REST API to retrieve the status of a credit card payment. The API contract is defined in:

```
hotel-reservation-connector/src/main/resources/Assignment02_creditcardpayment_api.yaml
```

The base URL is configured in `application.yaml`:

```yaml
credit-card-payment-api:
  base-url: <url-to-the-credit-card-payment-api>
```

### Option A — Use the Postman Mock Server (default)

A Postman mock server URL is pre-configured in `application.yaml`:

```yaml
credit-card-payment-api:
  base-url: https://eefcd287-c5b1-416b-9323-632244c15778.mock.pstmn.io/host/credit-card-payment-api
```

This mock returns fixed responses based on the request payload. It requires an active internet connection. You can replace this URL with your own Postman mock server if needed.

### Option B — Run WireMock Standalone

1. Download the WireMock standalone JAR from https://wiremock.org/docs/running-standalone/

2. Create a `mappings/` directory and add a mapping file, for example `credit-card-payment-stub.json`:

```json
{
  "request": {
    "method": "POST",
    "url": "/host/credit-card-payment-api/payment-status"
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "jsonBody": {
      "status": "CONFIRMED",
      "lastUpdateDate": "2024-01-15T10:30:00"
    }
  }
}
```

3. Start WireMock on port 9090:

```bash
java -jar wiremock-standalone-*.jar --port 9090
```

4. Update `application.yaml` to point to the local WireMock server:

```yaml
credit-card-payment-api:
  base-url: http://localhost:9090/host/credit-card-payment-api
```

5. To simulate a **rejected** payment, change `"status"` to `"REJECTED"` in the mapping file and restart WireMock (or use WireMock's admin API to update the mapping at runtime).

**Note:** An integration test is added using wiremock to simulate and test the applicable scenarios.

### Option C — Implement the Real API

Replace the `base-url` with the actual service endpoint and ensure the request/response contract matches the OpenAPI specification.

**Note:** This is not implemented in this project.

---

## 6. Kafka Setup

The application consumes events from the `bank-transfer-payment-update` topic.

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: hotel-reservation-group
```

### Step 1 — Start Kafka locally

Using Docker:

```bash
docker run -d --name kafka \
  -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  apache/kafka:latest
```

Alternatively, download Apache Kafka from https://kafka.apache.org/downloads and run:

**Note**: This is my preferred approach to test it

```bash
# Start Kafka with KRaft (no Zookeeper needed in Kafka 3.3+)
bin/kafka-server-start.sh config/kraft/server.properties
```

### Step 2 — Create the topic

```bash
# Using Docker
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --create --topic bank-transfer-payment-update \
  --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# Using local Kafka installation
bin/kafka-topics.sh --create --topic bank-transfer-payment-update \
  --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### Step 3 — Verify the topic exists

```bash
# Docker
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --list --bootstrap-server localhost:9092

# Local
bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Kafka Event Format

The listener expects messages on `bank-transfer-payment-update` as JSON:

```json
{
  "paymentId": "PAY-001",
  "debtorAccountNumber": "NL91ABNA0417164300",
  "amountReceived": "250.00",
  "transactionDescription": "E2EREFERAB RES12345"
}
```

**`transactionDescription` format:** `<10-char E2E ID> <8-char reservationId>`

The last 8 characters (after the space) are the reservation ID used to look up the booking in the database.

### Publish a Test Event Manually

```bash
# Docker
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh \
  --topic bank-transfer-payment-update \
  --bootstrap-server localhost:9092 \
  --property value.serializer=org.apache.kafka.common.serialization.StringSerializer

# Then type (or paste) the JSON payload and press Enter:
{"paymentId":"PAY-001","debtorAccountNumber":"NL91ABNA0417164300","amountReceived":"250.00","transactionDescription":"E2EREFERAB <reservationId>"}
```

Replace `<reservationId>` with an actual ID returned by the REST endpoint.

---

## 7. Database Management

The application uses an **H2 in-memory database**. No installation is required.

### Key Facts

| Property        | Value                                                               |
|-----------------|---------------------------------------------------------------------|
| JDBC URL        | `jdbc:h2:mem:mydb;DB_CLOSE_DELAY=-1`                                |
| Username        | `sa`                                                                |
| Password        | `password`                                                          |
| Dialect         | `org.hibernate.dialect.H2Dialect`                                   |
| Schema creation | Automatic (Hibernate DDL)                                           |
| Persistence     | **None** — data is lost on restart as it uses in-memory H2 database |

**Note:** A persistence unit using real database implementation is recommended. 
Although I did not get enough time to work on it, my recommended approach would be to use liquibase to manage the database tables, columns and schema.
- The database (e.g., PostgreSQL, Microsoft SQL Server) can be created using Docker.

### Enabling the H2 Console

Add the following to `application.yaml` to access the H2 web console at runtime:

```yaml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console
```

Then open: `http://localhost:8080/roomreservationapi/v1/h2-console`

Log in with:
- JDBC URL: `jdbc:h2:mem:mydb`
- Username: `check above`
- Password: `check above`

### Querying Reservations

Once connected via the H2 console (or any SQL client via the JDBC URL), you can run some queries (for example):

```sql 
-- List all reservations
SELECT * FROM ROOM;

-- Filter by status
SELECT * FROM ROOM WHERE STATUS = 'PENDING_PAYMENT';

-- Find a specific reservation
SELECT * FROM ROOM WHERE RESERVATION_ID = 'ABC12345';
```

### Switching to a Persistent Database

To use a persistent database such as PostgreSQL instead of H2:

1. Add the PostgreSQL driver to `hotel-reservation-app/pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

2. Update `application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hotelreservation
    username: your_user
    password: your_password
    driverClassName: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
```

---

## 8. Testing the Functionalities

### Prerequisites

- Application is running on `http://localhost:8080`
- Kafka is running (for bank transfer tests)
- Credit Card API is reachable (Postman mock or WireMock)

### Functionality 1 — Confirm a Reservation (REST Endpoint)

**Endpoint:** `POST http://localhost:8080/roomreservationapi/v1/confirm-reservation`

**Headers:**
- `Content-Type: application/json`
- `Trace-Id: <optional UUID>` — auto-generated if not provided

#### Test Case 1: Cash Payment (Immediate Confirmation)

**Request:**
```json
{
  "customerName": "John Doe",
  "roomNumber": 101,
  "startDate": "2026-06-01",
  "endDate": "2026-06-05",
  "roomSegment": "LARGE",
  "paymentMode": "CASH",
  "paymentReference": "CASH-REF-001"
}
```

**Expected Response (200 OK):**
```json
{
  "reservationId": "RES12345",
  "status": "CONFIRMED"
}
```

#### Test Case 2: Bank Transfer (Pending Payment)

**Request:**
```json
{
  "customerName": "John Doe",
  "roomNumber": 202,
  "startDate": "2026-05-10",
  "endDate": "2026-05-15",
  "roomSegment": "MEDIUM",
  "paymentMode": "BANK_TRANSFER",
  "paymentReference": "BT-REF-002"
}
```

**Expected Response (200 OK):**
```json
{
  "reservationId": "RES456AB",
  "status": "PENDING_PAYMENT"
}
```

Save the `reservationId` — you will need it to publish a Kafka event in Functionality 2.

#### Test Case 3: Credit Card Payment (Confirmed)

**Request:**
```json
{
  "customerName": "John Doe",
  "roomNumber": 303,
  "startDate": "2026-04-07",
  "endDate": "2026-04-10",
  "roomSegment": "SMALL",
  "paymentMode": "CREDIT_CARD",
  "paymentReference": "CC-REF-003"
}
```

**Expected Response (200 OK)** — requires the Credit Card API to return `CONFIRMED`:
```json
{
  "reservationId": "RES000178",
  "status": "CONFIRMED"
}
```

#### Test Case 4: Validation — Duration Exceeds 30 Days

**Request:**
```json
{
  "customerName": "John Doe",
  "roomNumber": 404,
  "startDate": "2026-04-07",
  "endDate": "2026-06-06",
  "roomSegment": "EXTRA_LARGE",
  "paymentMode": "CASH",
  "paymentReference": "CASH-REF-004"
}
```

**Expected Response (400 Bad Request):**
```json
{
  "errorMessage": "A room cannot be reserved for more than 30 days."
}
```

---

### Functionality 2 — Confirm Reservation via Kafka Bank Transfer Event

**Prerequisites:** Complete Test Case 2 above and note the `reservationId` (e.g., `RES12345`).

#### Step 1 — Verify the reservation is PENDING_PAYMENT in H2

Go to the H2 console and run:
```sql
SELECT * FROM ROOM WHERE RESERVATION_ID = 'RES12345';
-- STATUS should be PENDING_PAYMENT
```

#### Step 2 — Publish a Kafka event

**Note:** It can be either via Docker or Kafka standalone

```bash
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh \
  --topic bank-transfer-payment-update \
  --bootstrap-server localhost:9092
```

Type the following JSON (replace `RES12345` with the actual reservation ID):
```json
{"paymentId":"PAY-001","debtorAccountNumber":"NL91ABNA0417164300","amountReceived":"250.00","transactionDescription":"E2EREFERAB RES12345"}
```

Press Enter, then Ctrl+C to exit the producer.

#### Step 3 — Verify the reservation is now CONFIRMED

```sql
SELECT * FROM ROOM WHERE RESERVATION_ID = 'RES12345';
-- STATUS should now be CONFIRMED
```

---

### Functionality 3 — Auto-Cancellation of Unpaid Bank Transfer Reservations

The scheduler runs daily at midnight (`0 0 0 * * *`) and cancels any `PENDING_PAYMENT` bank-transfer reservation whose `startDate` is within 2 days.

#### Manual Trigger via Configuration (for testing)

To test without waiting for midnight, temporarily change the cron expression in `application.yaml` to run every minute:

```yaml
reservation:
  cancellation:
    deadlineDays: 2
    cron: "0 * * * * *"   # every minute — for testing only
```

#### Step-by-Step Test

1. Create a bank transfer reservation with a `startDate` set to **today or tomorrow** (within the 2-day deadline):

```json
{
  "customerName": "John Doe",
  "roomNumber": 505,
  "startDate": "<today or tomorrow's date>",
  "endDate": "<startDate + 5 days>",
  "roomSegment": "LARGE",
  "paymentMode": "BANK_TRANSFER",
  "paymentReference": "BT-REF-AUTO"
}
```

2. Confirm the reservation is `PENDING_PAYMENT` in H2.
3. Wait for the scheduler to fire (or use the every-minute cron above).
4. Check the reservation status — it should be `CANCELLED`:

```sql
SELECT * FROM ROOM WHERE CUSTOMER_NAME = 'John Doe';
-- STATUS should be CANCELLED
```

---

## 9. Running the Tests

### Run All Tests

```bash
./mvnw test
```

### Run Only Unit Tests (skip integration tests)

```bash
./mvnw test -Dgroups="!integration"
```

### Run Integration Tests Only

Integration tests are in `hotel-reservation-app` and use:
- `@SpringBootTest` with a random port
- **WireMock** to stub the Credit Card Payment API (no internet needed)

```bash
./mvnw test -f hotel-reservation-app/pom.xml
```

### Test Coverage by Module

| Module                        | Test Type        | Covers                                                    |
|-------------------------------|------------------|-----------------------------------------------------------|
| `hotel-reservation-repository`| Unit             | JPA queries, pessimistic locking                          |
| `hotel-reservation-connector` | Unit             | Retry logic, timeout, response mapping, Trace-Id header   |
| `hotel-reservation-service`   | Unit             | Payment routing, ID generation, auto-cancellation logic   |
| `hotel-reservation-app`       | Unit + Integration | Controller, Kafka listener, filters, exception handler  |

---

## 10. Configuration Reference

All configuration is in `hotel-reservation-app/src/main/resources/application.yaml`