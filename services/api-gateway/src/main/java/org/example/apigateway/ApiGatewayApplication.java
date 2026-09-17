package org.example.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri("http://auth-service:8081"))
                .route("product-service", r -> r
                        .path("/products/**")
                        .uri("http://product-service:8082"))
                .route("inventory-service", r -> r
                        .path("/inventory/**")
                        .uri("http://inventory-service:8083"))
                .route("cart-service", r -> r
                        .path("/cart/**")
                        .uri("http://cart-service:8084"))
                .route("order-service", r -> r
                        .path("/orders/**")
                        .uri("http://order-service:8085"))
                .route("payment-service", r -> r
                        .path("/payments/**")
                        .uri("http://payment-service:8086"))
                .build();
    }
}

