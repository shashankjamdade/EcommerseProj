package org.example.productservice.repository;

import org.example.productservice.model.ReviewEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface ReviewRepository extends ReactiveCrudRepository<ReviewEntity, UUID> {

    Flux<ReviewEntity> findAllByProductIdOrderByCreatedAtDesc(UUID productId);

    Mono<Long> countByProductId(UUID productId);

    @Query("SELECT COALESCE(AVG(rating), 0) FROM product_reviews WHERE product_id = :productId")
    Mono<BigDecimal> averageRating(@Param("productId") UUID productId);
}

