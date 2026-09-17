# API Documentation

## Overview

Each microservice exposes a REST API documented via OpenAPI 3.0 (Swagger). APIs follow REST conventions and return JSON responses.

## API Gateway - Port 8080

### Base URL
```
http://localhost:8080
```

### Endpoints

#### Health Check
```
GET /actuator/health
```

#### API Routes (Proxied to services)
```
GET  /auth/**              → Auth Service
GET  /products/**          → Product Service  
GET  /inventory/**         → Inventory Service
GET  /cart/**              → Cart Service
GET  /orders/**            → Order Service
POST /payments/**          → Payment Service
```

---

## Auth Service - Port 8081

### Base URL
```
http://localhost:8081
```

### Authentication
Uses JWT tokens in `Authorization: Bearer {token}` header.

### Endpoints

#### Register User
```
POST /api/v1/auth/register
Content-Type: application/json

Request:
{
  "email": "user@example.com",
  "password": "securePassword123",
  "firstName": "John",
  "lastName": "Doe"
}

Response (201):
{
  "id": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

#### Login
```
POST /api/v1/auth/login
Content-Type: application/json

Request:
{
  "email": "user@example.com",
  "password": "securePassword123"
}

Response (200):
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

#### Validate Token
```
POST /api/v1/auth/validate
Content-Type: application/json
Authorization: Bearer {token}

Response (200):
{
  "valid": true,
  "userId": "uuid",
  "email": "user@example.com"
}
```

---

## Product Service - Port 8082

### Base URL
```
http://localhost:8082
```

### Endpoints

#### Get All Products
```
GET /api/v1/products
Query Parameters:
  - page=0 (default: 0)
  - size=20 (default: 20)
  - category=electronics (optional)
  - search=laptop (optional)

Response (200):
{
  "content": [
    {
      "id": "uuid",
      "name": "MacBook Pro",
      "description": "High-performance laptop",
      "price": 1999.99,
      "category": "electronics",
      "availableQuantity": 10,
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0
}
```

#### Get Product by ID
```
GET /api/v1/products/{id}

Response (200):
{
  "id": "uuid",
  "name": "MacBook Pro",
  "description": "High-performance laptop",
  "price": 1999.99,
  "category": "electronics",
  "availableQuantity": 10,
  "images": ["url1", "url2"],
  "reviews": [
    {
      "userId": "uuid",
      "rating": 5,
      "comment": "Excellent product!",
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "createdAt": "2024-01-01T00:00:00Z"
}
```

#### Create Product (Admin only)
```
POST /api/v1/products
Authorization: Bearer {admin-token}
Content-Type: application/json

Request:
{
  "name": "New Product",
  "description": "Description",
  "price": 99.99,
  "category": "electronics",
  "stock": 50
}

Response (201):
{
  "id": "uuid",
  "name": "New Product",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

---

## Cart Service - Port 8084

### Base URL
```
http://localhost:8084
```

### Endpoints

#### Get Cart
```
GET /api/v1/cart
Authorization: Bearer {token}

Response (200):
{
  "id": "uuid",
  "userId": "uuid",
  "items": [
    {
      "productId": "uuid",
      "productName": "MacBook Pro",
      "quantity": 1,
      "price": 1999.99,
      "subtotal": 1999.99
    }
  ],
  "totalItems": 1,
  "grandTotal": 1999.99,
  "lastModified": "2024-01-01T00:00:00Z"
}
```

#### Add to Cart
```
POST /api/v1/cart/items
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "productId": "uuid",
  "quantity": 1
}

Response (201):
{
  "cartId": "uuid",
  "itemId": "uuid",
  "productId": "uuid",
  "quantity": 1,
  "addedAt": "2024-01-01T00:00:00Z"
}
```

#### Remove from Cart
```
DELETE /api/v1/cart/items/{itemId}
Authorization: Bearer {token}

Response (204): No Content
```

#### Clear Cart
```
DELETE /api/v1/cart
Authorization: Bearer {token}

Response (204): No Content
```

---

## Order Service - Port 8085

### Base URL
```
http://localhost:8085
```

### Endpoints

#### Create Order
```
POST /api/v1/orders
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "cartId": "uuid",
  "shippingAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  },
  "billingAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  }
}

Response (201):
{
  "orderId": "uuid",
  "userId": "uuid",
  "status": "PENDING",
  "totalAmount": 1999.99,
  "items": [...],
  "createdAt": "2024-01-01T00:00:00Z"
}
```

#### Get Order
```
GET /api/v1/orders/{orderId}
Authorization: Bearer {token}

Response (200):
{
  "orderId": "uuid",
  "userId": "uuid",
  "status": "CONFIRMED",
  "totalAmount": 1999.99,
  "items": [...],
  "shippingAddress": {...},
  "trackingNumber": "TRK123456",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

#### Get User Orders
```
GET /api/v1/orders
Authorization: Bearer {token}

Response (200):
{
  "content": [
    {
      "orderId": "uuid",
      "status": "CONFIRMED",
      "totalAmount": 1999.99,
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

---

## Payment Service - Port 8086

### Base URL
```
http://localhost:8086
```

### Endpoints

#### Process Payment
```
POST /api/v1/payments
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "orderId": "uuid",
  "amount": 1999.99,
  "currency": "USD",
  "paymentMethod": {
    "type": "CREDIT_CARD",
    "cardNumber": "4111111111111111",
    "expiryMonth": 12,
    "expiryYear": 2025,
    "cvv": "123"
  }
}

Response (201):
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "status": "CONFIRMED",
  "amount": 1999.99,
  "transactionId": "txn_123456",
  "processedAt": "2024-01-01T00:00:00Z"
}
```

---

## Inventory Service - Port 8083

### Base URL
```
http://localhost:8083
```

### Endpoints

#### Get Inventory
```
GET /api/v1/inventory/{productId}

Response (200):
{
  "productId": "uuid",
  "totalStock": 100,
  "reservedStock": 10,
  "availableStock": 90,
  "warehouses": [
    {
      "id": "uuid",
      "name": "Main Warehouse",
      "stock": 90
    }
  ]
}
```

---

## Notification Service - Port 8087

### Base URL
```
http://localhost:8087
```

### Endpoints

#### Get Notifications
```
GET /api/v1/notifications
Authorization: Bearer {token}

Response (200):
{
  "content": [
    {
      "id": "uuid",
      "type": "ORDER_CONFIRMED",
      "title": "Order Confirmed",
      "message": "Your order has been confirmed",
      "read": false,
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "totalElements": 10
}
```

#### Mark Notification as Read
```
PUT /api/v1/notifications/{notificationId}/read
Authorization: Bearer {token}

Response (200):
{
  "id": "uuid",
  "read": true
}
```

---

## Error Responses

All services return errors in consistent format:

```
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Invalid request parameters",
    "details": {
      "field": "email",
      "issue": "Invalid email format"
    },
    "timestamp": "2024-01-01T00:00:00Z",
    "path": "/api/v1/products"
  }
}
```

### Common Error Codes

| Code | Status | Description |
|------|--------|-------------|
| BAD_REQUEST | 400 | Invalid request parameters |
| UNAUTHORIZED | 401 | Missing or invalid authentication |
| FORBIDDEN | 403 | Insufficient permissions |
| NOT_FOUND | 404 | Resource not found |
| CONFLICT | 409 | Resource conflict |
| INTERNAL_ERROR | 500 | Internal server error |

---

## Rate Limiting

API Gateway enforces rate limiting:
- **Default**: 1000 requests per minute per IP
- **Authenticated**: 5000 requests per minute per user
- **Rate Limit Headers**:
  - `X-RateLimit-Limit`: Total allowed requests
  - `X-RateLimit-Remaining`: Remaining requests
  - `X-RateLimit-Reset`: Unix timestamp when limit resets

---

## Pagination

List endpoints support pagination:
```
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0,
  "pageSize": 20,
  "hasNextPage": true,
  "hasPreviousPage": false
}
```

---

## Versioning

APIs follow semantic versioning:
- Current version: **v1**
- URL path: `/api/v1/`
- Backwards compatibility maintained within major version

---

## Swagger/OpenAPI Documentation

Interactive API documentation available at each service:

- **API Gateway**: `http://localhost:8080/swagger-ui.html`
- **Auth Service**: `http://localhost:8081/swagger-ui.html`
- **Product Service**: `http://localhost:8082/swagger-ui.html`
- **Inventory Service**: `http://localhost:8083/swagger-ui.html`
- **Cart Service**: `http://localhost:8084/swagger-ui.html`
- **Order Service**: `http://localhost:8085/swagger-ui.html`
- **Payment Service**: `http://localhost:8086/swagger-ui.html`
- **Notification Service**: `http://localhost:8087/swagger-ui.html`

