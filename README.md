# Ticketing System

Backend for booking event tickets, split into Spring Boot microservices. I built it to practice
service-to-service communication with REST and Kafka, securing an API gateway with Keycloak, and
running the infrastructure with Docker Compose.

## How it works

1. The client sends a booking request to the API gateway.
2. The gateway checks the JWT and forwards the request to the booking service.
3. The booking service checks the customer and asks the inventory service if there are enough tickets.
4. If there are, it publishes a `BookingEvent` to the `booking-events` Kafka topic.
5. The order service consumes the event and asks the inventory service to lower the event capacity.
6. Only when the capacity update succeeds, the order service saves the order. If the event is sold
   out in the meantime, the order is rejected and nothing is saved.

```
Client --> API Gateway :8090
               |
       +-------+--------+
       v                v
 booking :8081 ---> inventory :8080
       |                ^
       | Kafka          | REST
       v                |
 order :8082 -----------+
       |
       v
   MySQL :3306
```

## Services

| Service           | Port | Description                                              |
|-------------------|------|----------------------------------------------------------|
| apigateway        | 8090 | routing, JWT validation, circuit breaker, Swagger UI     |
| inventory-service | 8080 | venues, events and remaining capacity                    |
| booking-service   | 8081 | validates bookings and publishes events to Kafka         |
| order-service     | 8082 | consumes booking events, updates inventory, saves orders |

## Tech stack

- Java 21, Spring Boot 4, Maven (multi-module)
- Spring Cloud Gateway (Server MVC), Resilience4j
- Spring Data JPA, MySQL, Flyway
- Spring Kafka
- Spring Security OAuth2 Resource Server, Keycloak
- Bean Validation, `ProblemDetail` error responses
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, AssertJ
- Docker Compose, Lombok

## Features

| Feature                  | How                                                                        |
|--------------------------|----------------------------------------------------------------------------|
| Single entry point       | Spring Cloud Gateway routes all requests on port 8090                      |
| Security                 | every API request needs a JWT from Keycloak                                |
| Event-driven orders      | booking and order service communicate over Kafka                           |
| No overbooking           | capacity is lowered with one atomic SQL update (`left_capacity >= n`)      |
| Fault tolerance          | circuit breaker on the booking route with a fallback response              |
| Clear error responses    | `ProblemDetail` JSON with 400 / 404 / 409 / 503                            |
| Request validation       | `@Valid` with `@NotNull` and `@Positive` on the booking request            |
| Database migrations      | Flyway creates the tables and inserts sample data on startup              |
| API documentation        | Swagger UI per service and aggregated in the gateway                       |

## Running locally

You need JDK 21 and Docker Desktop.

Start the infrastructure (MySQL, ZooKeeper, Kafka, Kafka UI, Schema Registry, Keycloak with its
own MySQL database):

```bash
docker compose up -d
```

Wait until Keycloak has started and imported the realm:

```bash
docker compose logs -f keycloak
```

Then start the services from the IDE in this order:

1. inventory-service (Flyway creates the tables and the sample data)
2. booking-service
3. order-service
4. apigateway

### Sample data

Flyway inserts this data, so you can book tickets right away:

| Table    | id | Data                                            |
|----------|----|-------------------------------------------------|
| venue    | 1  | Stadion Tehelne Pole, Bratislava (22,500 seats) |
| event    | 1  | BMTH Concert, 22,000 tickets, 99.99 per ticket  |
| customer | 1  | Jozko Mrkvicka                                  |

## Authentication

Every request through the gateway needs a bearer token from Keycloak, except the Swagger UI and
API docs.

- Keycloak admin console: http://localhost:8091 (admin / admin)
- Realm: `ticketing-security-realm`
- Grant type: Client Credentials
- Client ID: `ticketing-client-id`
- Client secret: in `docker/keycloak/realms/ticketing-security-realm.json`

> The client secret and all passwords in this repository are for local development only.

The realm is imported automatically on the first start of the Keycloak container. Keycloak stores
its data in its own MySQL container, so the realm survives `docker compose down`.

In Postman, use Authorization > OAuth 2.0 > Get New Access Token with:

| Field          | Value                                                                              |
|----------------|------------------------------------------------------------------------------------|
| Grant type     | Client Credentials                                                                 |
| Access token URL | `http://localhost:8091/realms/ticketing-security-realm/protocol/openid-connect/token` |
| Client ID      | `ticketing-client-id`                                                              |
| Client secret  | from the realm file                                                                |

## API

All requests go through the gateway on port 8090.

| Method | Endpoint                                          | Description                    |
|--------|---------------------------------------------------|--------------------------------|
| GET    | `/api/v1/inventory/events`                        | list all events                |
| GET    | `/api/v1/inventory/event/{eventId}`               | one event with capacity, price |
| GET    | `/api/v1/inventory/venue/{venueId}`               | venue details                  |
| PUT    | `/api/v1/inventory/event/{eventId}/capacity/{n}`  | lower the capacity by n        |
| POST   | `/api/v1/booking`                                 | create a booking               |

Example booking request:

```json
{
  "userId": 1,
  "eventId": 1,
  "ticketCount": 2
}
```

### Error responses

Errors are returned as `ProblemDetail` JSON:

| Status | When                                                        |
|--------|-------------------------------------------------------------|
| 400    | invalid request, e.g. missing `eventId` or `ticketCount` ≤ 0 |
| 401    | missing or invalid JWT                                      |
| 404    | customer, event or venue not found                          |
| 409    | not enough tickets                                          |
| 503    | inventory service not reachable, or booking service down (circuit breaker fallback) |

## API documentation

| Swagger UI                  | URL                                   |
|-----------------------------|---------------------------------------|
| All services (via gateway)  | http://localhost:8090/swagger-ui.html |
| Inventory service           | http://localhost:8080/swagger-ui.html |
| Booking service             | http://localhost:8081/swagger-ui.html |

The gateway page has a dropdown in the top right corner to switch between services.

## Monitoring

| Endpoint                              | Description                                |
|---------------------------------------|--------------------------------------------|
| http://localhost:8090/actuator/health | gateway health incl. circuit breaker state |

The circuit breaker appears in the health response after the first request to the booking route.

## Tests

The inventory and booking services have unit tests with JUnit 5 and Mockito. They don't need
Docker:

```bash
./mvnw test -Dtest='*ServiceTest' -Dsurefire.failIfNoSpecifiedTests=false
```

## Other local tools

| Tool                      | URL                                     |
|---------------------------|-----------------------------------------|
| Kafka UI                  | http://localhost:8084                   |
| Keycloak                  | http://localhost:8091                   |

## Known trade-offs

- **Shared database:** all services use one MySQL database (`ticketing`). In a real microservice
  setup, each service would own its database.
- **Booking is confirmed before the order is processed:** the booking service answers right after
  publishing the Kafka event. If the event sells out before the order service handles it, the
  order is rejected, but the client has already received a success response.
- **No distributed transaction:** if the capacity update succeeds but saving the order fails, the
  tickets are subtracted without an order. A Saga or Outbox pattern would solve this.

## Roadmap

- [x] Multi-module Maven setup with four services
- [x] Kafka event flow between booking and order service
- [x] API gateway with Keycloak JWT security and circuit breaker
- [x] Flyway migrations, validation, global error handling
- [x] Unit tests for inventory and booking services
- [ ] Unit tests for the order service and controller tests
- [ ] Dockerfiles for all services
- [ ] CI pipeline with GitHub Actions
- [ ] Integration tests with Testcontainers
