package org.example.productservice.repository;

import org.example.productservice.model.ProductEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductRepository extends ReactiveCrudRepository<ProductEntity, UUID> {

    @Query("""
            SELECT * FROM products
            WHERE active = TRUE
              AND (:categoryId IS NULL OR category_id = :categoryId)
              AND (:sellerUserId IS NULL OR seller_id = :sellerUserId)
              AND (:minPrice IS NULL OR price >= :minPrice)
              AND (:maxPrice IS NULL OR price <= :maxPrice)
              AND (:minRating IS NULL OR rating >= :minRating)
              AND (:searchTerm IS NULL
                   OR LOWER(name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                   OR LOWER(COALESCE(description, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            ORDER BY updated_at DESC
            """)
    Flux<ProductEntity> searchActiveProducts(@Param("categoryId") UUID categoryId,
                                             @Param("sellerUserId") UUID sellerUserId,
                                             @Param("minPrice") BigDecimal minPrice,
                                             @Param("maxPrice") BigDecimal maxPrice,
                                             @Param("minRating") BigDecimal minRating,
                                             @Param("searchTerm") String searchTerm);

    @Query("SELECT * FROM products ORDER BY updated_at DESC")
    Flux<ProductEntity> findAllOrdered();

    @Query("SELECT COUNT(*) FROM products WHERE category_id = :categoryId")
    Mono<Long> countByCategoryId(@Param("categoryId") UUID categoryId);

    @Query("SELECT COUNT(*) FROM products WHERE seller_id = :sellerUserId")
    Mono<Long> countBySellerId(@Param("sellerUserId") UUID sellerUserId);
}

