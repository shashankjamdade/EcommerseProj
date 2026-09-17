# E-Commerce Platform

A scalable, microservices-based e-commerce platform built with Spring Boot, deployed on Kubernetes with infrastructure-as-code.

## Architecture Overview

```
┌─────────────────┐
│   API Gateway   │ (Port 8080)
├─────────────────┤
│  Load Balancer  │
└────────┬────────┘
         │
    ┌────┴─────────────────────────┐
    │                              │
┌───▼────────┐  ┌───────────────┐  │  ┌──────────────┐
│Auth Service│  │Product Service│  │  │Cart Service  │
│(8081)      │  │(8082)         │  │  │(8084)        │
└────────────┘  └───────────────┘  │  └──────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
             ┌──────▼────────┐ ┌────▼─────────┐ ┌──▼─────────────┐
             │Order Service  │ │Payment Service│ │Notification    │
             │(8085)         │ │(8086)        │ │Service(8087)   │
             └────────────────┘ └──────────────┘ └────────────────┘
                    │               │
         ┌──────────┴───────────────┴──────────┐
         │       Message Queue (Kafka)         │
         └──────────────────────────────────────┘
         
         ┌──────────────────────────────────────┐
         │  Data Layer (PostgreSQL + Redis)     │
         └──────────────────────────────────────┘
```

## Microservices

### API Gateway
- **Port**: 8080
- **Responsibilities**: 
  - Request routing and load balancing
  - Rate limiting
  - Authentication/Authorization enforcement
- **Framework**: Spring Cloud Gateway

### Auth Service
- **Port**: 8081
- **Responsibilities**:
  - User authentication
  - JWT token generation
  - Role-based authorization
  - DB-backed sliding session expiry
- **Database**: PostgreSQL
- **Framework**: Spring WebFlux + Spring Security

### Product Service
- **Port**: 8082
- **Responsibilities**:
  - Product catalog management
  - Product search and filtering
  - Product details and reviews
- **Database**: PostgreSQL
- **Cache**: Redis

### Inventory Service
- **Port**: 8083
- **Responsibilities**:
  - Inventory tracking
  - Stock management
  - Warehouse operations
- **Database**: PostgreSQL
- **Messaging**: Kafka

### Cart Service
- **Port**: 8084
- **Responsibilities**:
  - Shopping cart management
  - Cart persistence
  - Cart operations
- **Cache**: Redis
- **Messaging**: Kafka

### Order Service
- **Port**: 8085
- **Responsibilities**:
  - Order creation and management
  - Order processing workflow
  - Order history
- **Database**: PostgreSQL
- **Messaging**: Kafka

### Payment Service
- **Port**: 8086
- **Responsibilities**:
  - Payment processing
  - Payment gateway integration
  - Transaction management
- **Database**: PostgreSQL
- **Messaging**: Kafka

### Notification Service
- **Port**: 8087
- **Responsibilities**:
  - Email notifications
  - SMS notifications
  - Push notifications
- **Messaging**: Kafka Consumer

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.2.0
- **Cloud Framework**: Spring Cloud 2023.0.0
- **API Gateway**: Spring Cloud Gateway
- **Database**: PostgreSQL 16
- **Cache**: Redis 7
- **Message Queue**: Apache Kafka 7.5.0
- **Orchestration**: Kubernetes (EKS)
- **Infrastructure**: Terraform
- **Container Registry**: ECR
- **CI/CD**: GitHub Actions

## Local Development

### Prerequisites

- Docker Desktop
- Java 21
- Maven 3.8+
- Docker Compose

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/ecommerce-platform.git
   cd ecommerce-platform
   ```

2. **Start infrastructure with Docker Compose**
   ```bash
   docker-compose -f infrastructure/docker/docker-compose.yml up -d
   ```

   This starts the shared local infrastructure only:
   - PostgreSQL: `localhost:5432`
   - Redis: `localhost:6379`
   - Kafka: `localhost:9092`
   - Cassandra: `localhost:9042`
   - MongoDB: `localhost:27017`
   - Elasticsearch: `localhost:9200`

   `auth-service` uses PostgreSQL reactively via R2DBC and stores JWT session expiry in the `auth_sessions` table.

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Start individual services in IntelliJ** (in separate terminals or run configs)
   ```bash
   # Start each service
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local" -pl services/auth-service
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local" -pl services/product-service
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local" -pl services/order-service
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local" -pl services/inventory-service
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local" -pl services/cart-service
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local" -pl services/payment-service
   ```

### IntelliJ Port Map

| Service | Port |
|---|---:|
| `auth-service` | `8081` |
| `product-service` | `8082` |
| `order-service` | `8083` |
| `inventory-service` | `8084` |
| `cart-service` | `8085` |
| `payment-service` | `8086` |

> Run these services locally in IntelliJ and point them at the Docker Compose infrastructure on `localhost`.

5. **Access API Gateway**
   ```
   http://localhost:8080
   ```

## Deployment

### Prerequisites for Production

- AWS Account with appropriate permissions
- Terraform installed (v1.0+)
- kubectl configured
- AWS CLI configured

### Deploy Infrastructure

```bash
cd infrastructure/terraform

# Initialize Terraform
terraform init

# Plan deployment
terraform plan -var-file=environments/prod.tfvars

# Apply infrastructure
terraform apply -var-file=environments/prod.tfvars
```

### Deploy Services

```bash
# Update kubeconfig
aws eks update-kubeconfig --name ecommerce-cluster --region us-east-1

# Deploy services
kubectl apply -f infrastructure/k8s/namespace.yaml
kubectl apply -f infrastructure/k8s/*/deployment.yaml
```

### CI/CD Pipeline

The project uses GitHub Actions for CI/CD:

1. **CI Pipeline** (`ci.yml`):
   - Builds all services
   - Runs unit tests
   - Performs security scanning
   - Builds and pushes Docker images to ECR

2. **Deploy Pipeline** (`deploy.yml`):
   - Deploys to EKS cluster
   - Runs health checks
   - Performs rollback on failure

## Configuration

### Environment Variables

Services use Spring profiles for environment configuration:

- `local`: Local development with default configs
- `docker`: Docker Compose environment
- `k8s`: Kubernetes deployment

### Database Configuration

Each service has its own PostgreSQL database:

- Auth Service: `auth_db`
- Product Service: `product_db`
- Inventory Service: `inventory_db`
- Order Service: `order_db`
- Payment Service: `payment_db`

### Auth Session Behavior

- Register with email, password, and one or more roles.
- Login with email, password, and the active role to receive a JWT bearer token.
- Protected APIs require `Authorization: Bearer <token>`.
- Sliding expiration is persisted in PostgreSQL, not returned as a refreshed token on every request.
- The auth service maintains `login_time`, `logout_time`, `expires_at`, and `is_expired` in `auth_sessions`.

Credentials are stored in Kubernetes Secrets or AWS Secrets Manager.

## Shared Libraries

### Common Library (`common-lib`)
Shared utilities and models used across all services.

### Event Contracts (`event-contracts-lib`)
Kafka event schemas and message contracts for async communication.

### Observability (`observability-lib`)
Centralized logging, metrics, and distributed tracing.

### Security (`security-lib`)
JWT utilities, authorization, and security configurations.

## Monitoring & Observability

### Metrics
All services expose Prometheus metrics at `/actuator/prometheus`

### Logging
Centralized logging using Spring Cloud Sleuth and ELK stack

### Tracing
Distributed tracing with Spring Cloud Sleuth and Jaeger

## API Documentation

API documentation is available via Swagger/OpenAPI at each service's `/swagger-ui.html` endpoint.

Example:
- API Gateway: `http://localhost:8080/swagger-ui.html`
- Auth Service: `http://localhost:8081/swagger-ui.html`

## Project Structure

```
ecommerce-platform/
├── services/              # All microservices
│   ├── api-gateway/
│   ├── auth-service/
│   ├── product-service/
│   ├── inventory-service/
│   ├── cart-service/
│   ├── order-service/
│   ├── payment-service/
│   └── notification-service/
├── libs/                  # Shared libraries
│   ├── common/
│   ├── event-contracts/
│   ├── observability/
│   └── security/
├── infrastructure/        # Infrastructure as Code
│   ├── docker/
│   ├── k8s/
│   └── terraform/
├── docs/                  # Documentation
└── pom.xml               # Parent POM
```

## Contributing

1. Create feature branch from `develop`
2. Commit changes
3. Push to branch
4. Create Pull Request
5. Wait for CI/CD pipeline to pass

## Testing

### Run all tests
```bash
mvn test
```

### Run tests for specific service
```bash
mvn test -pl services/order-service
```

### Run integration tests
```bash
mvn test -DskipUnitTests=true
```

## Troubleshooting

### Services not starting
- Check Docker containers: `docker-compose ps`
- Check logs: `docker-compose logs <service-name>`

### Kubernetes deployment issues
```bash
# Check pod status
kubectl get pods -n ecommerce

# Check pod logs
kubectl logs -n ecommerce <pod-name>

# Describe pod for events
kubectl describe pod -n ecommerce <pod-name>
```

## License

This project is licensed under the MIT License - see LICENSE file for details.

## Support

For issues, questions, or contributions, please open an issue on GitHub.

