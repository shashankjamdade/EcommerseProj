package org.example.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                           @Value("${services.auth-service.url:http://localhost:8081}") String authServiceUrl,
                                           @Value("${services.product-service.url:http://localhost:8082}") String productServiceUrl,
                                           @Value("${services.order-service.url:http://localhost:8083}") String orderServiceUrl,
                                           @Value("${services.inventory-service.url:http://localhost:8084}") String inventoryServiceUrl,
                                           @Value("${services.cart-service.url:http://localhost:8085}") String cartServiceUrl,
                                           @Value("${services.payment-service.url:http://localhost:8086}") String paymentServiceUrl) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .filters(f -> f.rewritePath("/auth/(?<segment>.*)", "/api/v1/auth/${segment}"))
                        .uri(authServiceUrl))
                .route("product-service", r -> r
                        .path("/products/**")
                        .uri(productServiceUrl))
                .route("cart-service", r -> r
                        .path("/cart/**")
                        .uri(cartServiceUrl))
                .route("order-service", r -> r
                        .path("/orders/**")
                        .uri(orderServiceUrl))
                .route("inventory-service", r -> r
                        .path("/inventory/**")
                        .uri(inventoryServiceUrl))
                .route("payment-service", r -> r
                        .path("/payments/**")
                        .uri(paymentServiceUrl))
                .build();
    }
}

