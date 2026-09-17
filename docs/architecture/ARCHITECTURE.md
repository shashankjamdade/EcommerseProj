# System Architecture

## Overview

The E-Commerce Platform is built using a microservices architecture where each business domain operates as an independent service that can be deployed, scaled, and maintained separately.

## High Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                           Clients                               │
│                    (Web, Mobile, API)                           │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                   ┌───────▼────────┐
                   │  API Gateway   │
                   │ (Port: 8080)   │
                   └───────┬────────┘
                           │
            ┌──────────────┼──────────────┐
            │              │              │
    ┌───────▼──┐   ┌──────▼─────┐  ┌────▼────────┐
    │   Auth   │   │  Product   │  │   Inventory │
    │ Service  │   │  Service   │  │   Service   │
    │(8081)    │   │  (8082)    │  │   (8083)    │
    └──────────┘   └────────────┘  └─────────────┘
            │              │              │
            ├──────────────┼──────────────┤
            │              │              │
    ┌───────▼──┐   ┌──────▼─────┐  ┌────▼────────┐
    │   Cart   │   │   Order    │  │  Payment    │
    │ Service  │   │  Service   │  │   Service   │
    │(8084)    │   │  (8085)    │  │   (8086)    │
    └──────────┘   └────────────┘  └─────────────┘
            │              │              │
            └──────────────┼──────────────┘
                           │
                   ┌───────▼────────┐
                   │    Kafka       │
                   │ Message Queue  │
                   └────────────────┘
                           │
                           │
                   ┌───────▼──────────┐
                   │ Notification     │
                   │ Service (8087)   │
                   └──────────────────┘

┌──────────────────────────────────────────────────┐
│              Data Layer                          │
│  ┌─────────────┐  ┌────────┐  ┌──────────────┐  │
│  │  PostgreSQL │  │ Redis  │  │   Kafka      │  │
│  │ (Multi-DB)  │  │ Cache  │  │  Broker      │  │
│  └─────────────┘  └────────┘  └──────────────┘  │
└──────────────────────────────────────────────────┘
```

## Component Responsibilities

### API Gateway
Acts as the single entry point for all external requests.

**Responsibilities**:
- Request routing to appropriate services
- Load balancing
- Rate limiting
- Request/Response logging
- Authentication token validation
- CORS handling

**Technologies**: Spring Cloud Gateway

---

### Auth Service
Manages user authentication and authorization.

**Responsibilities**:
- User registration and login
- JWT token generation and validation
- OAuth2/OIDC integration
- Role and permission management
- Session management

**Technologies**: Spring Boot, Spring Security, PostgreSQL
**Exports**: User context, JWT tokens

---

### Product Service
Manages product catalog and information.

**Responsibilities**:
- Product CRUD operations
- Product search and filtering
- Product categorization
- Review and rating management
- Inventory availability check

**Technologies**: Spring Boot, PostgreSQL, Redis (caching)
**Consumes Events**: Inventory updated
**Publishes Events**: Product created, updated, deleted

---

### Inventory Service
Manages product stock and warehouse operations.

**Responsibilities**:
- Inventory tracking per warehouse
- Stock level management
- Inventory reservations
- Stock adjustment operations
- Low stock alerts

**Technologies**: Spring Boot, PostgreSQL, Kafka
**Consumes Events**: Order placed, Order cancelled, Payment confirmed
**Publishes Events**: Stock reserved, Stock released, Stock updated

---

### Cart Service
Manages shopping cart operations.

**Responsibilities**:
- Cart creation and persistence
- Add/remove items from cart
- Cart total calculation
- Cart abandonment tracking
- Cart recovery

**Technologies**: Spring Boot, Redis (session storage), Kafka
**Consumes Events**: Product updated, Inventory reserved
**Publishes Events**: Cart abandoned, Cart converted to order

---

### Order Service
Manages order processing workflow.

**Responsibilities**:
- Order creation from cart
- Order status management
- Order tracking and history
- Return and refund processing
- Order analytics

**Technologies**: Spring Boot, PostgreSQL, Kafka
**Consumes Events**: Payment confirmed, Payment failed, Shipment updated
**Publishes Events**: Order placed, Order confirmed, Order shipped

---

### Payment Service
Handles payment processing.

**Responsibilities**:
- Payment method management
- Payment processing
- Transaction recording
- Payment failure handling
- Refund processing

**Technologies**: Spring Boot, PostgreSQL, Kafka
**Consumes Events**: Order placed
**Publishes Events**: Payment confirmed, Payment failed

---

### Notification Service
Sends notifications to users.

**Responsibilities**:
- Email notifications
- SMS notifications
- Push notifications
- In-app notifications
- Notification history

**Technologies**: Spring Boot, Kafka, Mail Server
**Consumes Events**: User registered, Order placed, Order shipped, Payment confirmed

---

## Data Flow

### Happy Path - Order Creation

1. **Client** → **API Gateway**: POST /orders
2. **API Gateway** → **Cart Service**: Get cart items
3. **Cart Service** → **API Gateway**: Cart items
4. **API Gateway** → **Inventory Service**: Reserve items
5. **Inventory Service** → **API Gateway**: Reservation confirmed
6. **API Gateway** → **Order Service**: Create order
7. **Order Service** → **Kafka**: Publish "OrderPlaced" event
8. **Payment Service** → **Kafka**: Subscribe to "OrderPlaced"
9. **Payment Service** → **Payment Gateway**: Process payment
10. **Payment Service** → **Kafka**: Publish "PaymentConfirmed" event
11. **Order Service** → **Kafka**: Subscribe to "PaymentConfirmed"
12. **Order Service** → **Database**: Update order status
13. **Notification Service** → **Kafka**: Subscribe to "PaymentConfirmed"
14. **Notification Service** → **Email Server**: Send confirmation

---

## Technology Stack

### Programming
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.0
- **Cloud Framework**: Spring Cloud 2023.0.0

### Gateway & Routing
- **API Gateway**: Spring Cloud Gateway
- **Load Balancing**: Kubernetes Service

### Data
- **Relational DB**: PostgreSQL 16
- **Caching**: Redis 7
- **Messaging**: Apache Kafka 7.5

### Deployment
- **Container Runtime**: Docker
- **Orchestration**: Kubernetes (AWS EKS)
- **Container Registry**: Amazon ECR
- **Infrastructure**: Terraform

### CI/CD
- **Source Control**: GitHub
- **CI/CD**: GitHub Actions
- **Package Registry**: GitHub Container Registry (GHCR)

### Monitoring
- **Logging**: Spring Cloud Sleuth, Logback, ELK
- **Metrics**: Micrometer, Prometheus
- **Tracing**: Jaeger
- **Health Checks**: Spring Boot Actuator

---

## Deployment Architecture

### Local Development
- Docker Compose for all dependencies (PostgreSQL, Redis, Kafka)
- Services run on host machine with Spring profiles

### Production (AWS)

```
┌─────────────────────────────────────────────────┐
│              AWS Account                        │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │         VPC (10.0.0.0/16)                 │  │
│  │                                           │  │
│  │  ┌─────────────┐  ┌─────────────┐       │  │
│  │  │ Public      │  │ Private     │       │  │
│  │  │ Subnets     │  │ Subnets     │       │  │
│  │  └─────────────┘  └─────────────┘       │  │
│  │       │                    │             │  │
│  │       └────┬─────────┬─────┘             │  │
│  │            │         │                  │  │
│  │      ┌─────▼─────────▼────┐             │  │
│  │      │   EKS Cluster      │             │  │
│  │      │  (Multiple Nodes)  │             │  │
│  │      │                    │             │  │
│  │      │  ┌──────────────┐  │             │  │
│  │      │  │ Microservices│  │             │  │
│  │      │  │  (Pods)      │  │             │  │
│  │      │  └──────────────┘  │             │  │
│  │      └────────────────────┘             │  │
│  │            │                           │  │
│  │      ┌─────┴──────────┬────────────┐  │  │
│  │      │                │            │  │  │
│  │  ┌───▼──┐        ┌────▼──┐    ┌───▼──┐ │  │
│  │  │ RDS  │        │Redis  │    │Kafka │ │  │
│  │  │Multi │        │Cluster│    │      │ │  │
│  │  │DB    │        │       │    │      │ │  │
│  │  └──────┘        └───────┘    └──────┘ │  │
│  │                                       │  │
│  └───────────────────────────────────────┘  │
│                                             │
│  ┌───────────────────────────────────────┐  │
│  │         ECR                           │  │
│  │  (Container Image Registry)          │  │
│  └───────────────────────────────────────┘  │
│                                             │
└─────────────────────────────────────────────┘
```

---

## Scalability Considerations

### Horizontal Scaling
- Services are stateless and can be scaled horizontally in Kubernetes
- Database connections pooled via PgBouncer
- Redis Cache distributed across cluster

### Vertical Scaling
- Pod resource requests and limits configured
- Kubernetes autoscaler based on CPU/Memory metrics

### Database Scaling
- Read replicas for heavy read operations
- Connection pooling for efficiency
- Sharding strategy for future growth

---

## Security Considerations

### Network Security
- VPC with private subnets for services
- Security groups restricting traffic
- TLS encryption for inter-service communication

### Application Security
- JWT token-based authentication
- OAuth2 authorization
- Role-based access control (RBAC)
- Input validation and sanitization
- SQL injection prevention via ORM

### Data Security
- Encrypted database passwords in Secrets Manager
- Encryption at rest for RDS and EBS
- Encryption in transit via TLS
- VPC encryption

---

## Performance & Reliability

### Caching Strategy
- Product catalog cached in Redis
- Session data in Redis
- Cache invalidation on updates

### Resilience Patterns
- Circuit breakers for service calls
- Retries with exponential backoff
- Request timeouts
- Health check endpoints
- Graceful degradation

### High Availability
- Multi-AZ deployment for databases
- EKS across multiple availability zones
- Load balancing at multiple levels
- Auto-healing of failed pods

