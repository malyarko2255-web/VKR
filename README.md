# Advance Service

Web service for driver advance management in a logistics company.

## Architecture

Microservices monorepo with the following modules:

| Module | Port | Description |
|--------|------|-------------|
| advance-gateway | 8080 | API Gateway (Spring Cloud Gateway) |
| advance-identity | 8081 | Identity Service (OAuth2 Resource Server) |
| advance-core | 8082 | Core Advance Service (Business Logic) |
| advance-payment | 8083 | Payment Service with Outbox Pattern |
| advance-scoring | 8084 | Telematics Scoring Service |
| advance-reference | 8085 | Reference Data Service |
| advance-notification | 8086 | Notification Service |
| advance-frontend | 3000 | React PWA Frontend |

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- Node.js 20+ (for frontend)

## Build All Modules

```bash
mvn clean install -DskipTests
```

## Run a Single Service

```bash
cd advance-core
mvn spring-boot:run
```

## Frontend Setup

```bash
cd advance-frontend
npm install && npm start
```
