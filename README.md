# Ticketing System

Backend for booking event tickets, split into Spring Boot microservices. I built it to practice
service-to-service communication with REST and Kafka, securing an API gateway with Keycloak, and
running the infrastructure with Docker Compose.

## How it works

1. The client sends a booking request to the API gateway.
2. The gateway checks the JWT and forwards the request to the booking service.
3. The booking service checks the customer and asks the inventory service if there are enough tickets.
4. If there are, it publishes a `BookingEvent` to the `booking-events` Kafka topic.
5. The order service consumes the event, saves the order and calls the inventory service to lower
   the event capacity.

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
| order-service     | 8082 | consumes booking events, saves orders, updates inventory |

## Tech stack

- Java 21, Spring Boot 4, Maven (multi-module)
- Spring Cloud Gateway (Server MVC), Resilience4j
- Spring Data JPA, MySQL
- Spring Kafka
- Spring Security OAuth2 Resource Server, Keycloak
- springdoc-openapi (Swagger UI)
- Docker Compose, Lombok

## Running locally

You need JDK 21 and Docker Desktop.

Start the infrastructure (MySQL, ZooKeeper, Kafka, Kafka UI, Schema Registry, Keycloak):

```bash
docker compose up -d
```

Then start the services from the IDE in this order:

1. inventory-service
2. booking-service
3. order-service
4. apigateway

## Authentication

Every request through the gateway needs a bearer token from Keycloak, except the Swagger UI and
API docs.

- Keycloak admin console: http://localhost:8091 (admin / admin)
- Realm: `ticketing-security-realm`

The realm is not exported to this repository yet, so it has to be created manually in the admin
console together with a client that can issue tokens. In Postman, use
Authorization > OAuth 2.0 > Get New Access Token with the token URL
`http://localhost:8091/realms/ticketing-security-realm/protocol/openid-connect/token`.

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

## API documentation

| Swagger UI                  | URL                                   |
|-----------------------------|---------------------------------------|
| All services (via gateway)  | http://localhost:8090/swagger-ui.html |
| Inventory service           | http://localhost:8080/swagger-ui.html |
| Booking service             | http://localhost:8081/swagger-ui.html |

The gateway page has a dropdown in the top right corner to switch between services.

## Other local tools

| Tool                      | URL                                     |
|---------------------------|-----------------------------------------|
| Kafka UI                  | http://localhost:8084                   |
| Keycloak                  | http://localhost:8091                   |