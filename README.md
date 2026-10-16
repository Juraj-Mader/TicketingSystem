# Ticketing System

A microservices backend for booking event tickets, built with **Java 21** and **Spring Boot 4**.
A booking is checked against inventory, sent through **Kafka**, saved as an order, and the event capacity is updated.

## Architecture

```
Client ──► API Gateway :8090 (Keycloak JWT + circuit breaker)
               │
      ┌────────┴─────────┐
      ▼                  ▼
 booking :8081 ──REST──► inventory :8080
      │                  ▲
      │ Kafka            │ REST (update capacity)
      ▼ "booking-events" │
 order :8082 ────────────┘
      │
      ▼
   MySQL :3306
```

## Services

| Service | Port | Responsibility |
|---|---|---|
| api-gateway | 8090 | routing, security, circuit breaker |
| inventory-service | 8080 | venues, events, ticket capacity |
| booking-service | 8081 | validates bookings, publishes Kafka events |
| order-service | 8082 | consumes events, saves orders, updates capacity |

## Tech stack

Java 21 · Spring Boot 4 · Spring Cloud Gateway · Spring Data JPA · Spring Kafka ·
Resilience4j · Keycloak (OAuth2/JWT) · MySQL · Docker Compose · Maven · Lombok · springdoc OpenAPI

## How to run

**Requirements:** JDK 21, Docker Desktop, Maven (or the included `mvnw`)

1. Start the infrastructure:
```bash
   docker compose up -d
```
2. Start the services in this order: `inventory-service` → `booking-service` → `order-service` → `apigateway`

## Example requests

| Method | URL |
|---|---|
| GET | `http://localhost:8090/api/v1/inventory/events` |
| GET | `http://localhost:8090/api/v1/inventory/event/{eventId}` |
| POST | `http://localhost:8090/api/v1/booking` |

Booking request body:
```json
{ "userId": 1, "eventId": 1, "ticketCount": 2 }
```

## Local tools

| Tool | URL |
|---|---|
| Swagger UI (inventory) | http://localhost:8080/swagger-ui.html |
| Kafka UI | http://localhost:8084 |
| Keycloak | http://localhost:8091 |