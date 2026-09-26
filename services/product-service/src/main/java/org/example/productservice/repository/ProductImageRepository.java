package org.example.productservice.repository;

import org.example.productservice.model.ProductImageEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductImageRepository extends ReactiveCrudRepository<ProductImageEntity, UUID> {

    @Query("""
            SELECT * FROM product_images
            WHERE product_id = :productId
            ORDER BY is_primary DESC, display_order ASC, created_at ASC
            """)
    Flux<ProductImageEntity> findAllByProductIdOrdered(@Param("productId") UUID productId);

    Mono<ProductImageEntity> findByIdAndProductId(UUID id, UUID productId);
}
